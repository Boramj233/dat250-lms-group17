package no.hvl.dat250.lms.security;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import no.hvl.dat250.lms.security.login.LmsOidcUserService;
import no.hvl.dat250.lms.security.token.TokenRevokingLogoutHandler;
import no.hvl.dat250.lms.security.token.TokenService;

/**
 * Two filter chains. Spring Security picks the first chain (lowest {@code @Order}) whose
 * {@code securityMatcher} matches the request, and only that chain runs.
 *
 * <ol>
 *   <li>{@link #apiSecurity}: {@code /api/**}. Stateless; the only credential is
 *       {@code Authorization: Bearer <token>} with a token from GET /token.</li>
 *   <li>{@link #loginSecurity}: everything else. Spring Boot is an OAuth 2.0 client that
 *       lets Keycloak log the user in; the result is a session cookie, which is only
 *       good for fetching an API token from GET /token.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, TokenService tokens) throws Exception {
        http
            .securityMatcher("/api/**")
            // Never create a session and never read the SecurityContext from one: every
            // request authenticates itself with its token.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            // The credential is an explicit header, never a cookie, so CSRF is not possible.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // Bearer token -> TokenService.introspect -> AuthenticatedUser + ROLE_<access>.
            .oauth2ResourceServer(server -> server.opaqueToken(opaque -> opaque.introspector(tokens)))
            // Missing, invalid or expired token: 401 with WWW-Authenticate: Bearer, never a
            // redirect to the login page.
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain loginSecurity(HttpSecurity http, LmsOidcUserService oidcUsers,
                                      TokenRevokingLogoutHandler revokeTokens,
                                      @Value("${lms.security.issuer-uri}") String issuerUri,
                                      @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
                                      String clientId) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Exchanges the login session for an API token.
                .requestMatchers("/token").authenticated()
                // Start of the login (step 1) and Keycloak's redirect back (step 3).
                .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                // The SPA's static files contain no data, so they are open without a token
                // (in Docker nginx serves them; this covers serving them from the backend).
                .requestMatchers("/", "/index.html", "/assets/**",
                        "/favicon.ico", "/favicon.svg", "/icons.svg").permitAll()
                // Spring's error page.
                .requestMatchers("/error").permitAll()
                // Everything else (including the H2 console) is closed.
                .anyRequest().denyAll())
            .requestCache(cache -> cache.disable())
            // GET /token is called with fetch(): answer 401 instead of redirecting to Keycloak.
            // The SPA then shows its Login button, which navigates to the login page.
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .oauth2Login(login -> login
                // Without our own login page Spring would render a default one at /login.
                .loginPage("/oauth2/authorization/keycloak")
                // After the code exchange: find or create the local User by verified email.
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUsers))
                // Back to the SPA, on the address the browser used (Vite or nginx).
                .defaultSuccessUrl("/", true)
                .failureUrl("/?login_error"))
            // POST /logout: 1) revoke the user's API tokens, 2) invalidate the session
            // (Spring's own handler, which runs after the ones added here), 3) redirect to
            // Keycloak's logout; otherwise the next login would not ask for the password.
            .logout(logout -> logout
                .addLogoutHandler(revokeTokens)
                .logoutSuccessHandler(keycloakLogout(issuerUri, clientId)))
            // The session cookie is SameSite=Lax, so a cross-site form cannot send it to
            // /logout. The SPA has no CSRF token to send.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"));
        return http.build();
    }

    /** Redirects to Keycloak's end-session endpoint, which then returns to the SPA. */
    private static LogoutSuccessHandler keycloakLogout(String issuerUri, String clientId) {
        return (request, response, authentication) -> {
            String home = ServletUriComponentsBuilder.fromContextPath(request).path("/").toUriString();
            var uri = UriComponentsBuilder.fromUriString(issuerUri + "/protocol/openid-connect/logout")
                    .queryParam("client_id", clientId)
                    .queryParam("post_logout_redirect_uri", home);
            if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
                uri.queryParam("id_token_hint", user.getIdToken().getTokenValue());
            }
            response.sendRedirect(uri.encode().build().toUriString());
        };
    }

    /**
     * Validates the id_token Keycloak returns from the code exchange (step 4): signature
     * (keys from the internal JWK set URI), audience, expiry, and the issuer. The endpoints
     * are configured one by one (public and internal URL differ), so Spring has no issuer
     * to compare with; Keycloak writes its public URL into the iss claim.
     */
    @Bean
    JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory(
            @Value("${lms.security.issuer-uri}") String issuerUri) {
        var factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(registration -> new DelegatingOAuth2TokenValidator<>(
                new OidcIdTokenValidator(registration), new JwtIssuerValidator(issuerUri)));
        return factory;
    }

    /** Time source for token expiry; replaceable in tests. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** A higher access level includes everything the lower levels may do. */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("COURSE_MANAGER")
                .role("COURSE_MANAGER").implies("STANDARD")
                .build();
    }
}
