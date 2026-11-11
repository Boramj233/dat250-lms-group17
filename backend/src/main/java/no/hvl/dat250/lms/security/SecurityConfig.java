package no.hvl.dat250.lms.security;

import java.util.List;
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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import no.hvl.dat250.lms.security.login.LmsOidcUserService;
import no.hvl.dat250.lms.security.token.LmsJwtAuthenticationConverter;

/**
 * Two filter chains. Spring Security picks the first chain (lowest {@code @Order}) whose
 * {@code securityMatcher} matches the request, and only that chain runs.
 *
 * <ol>
 *   <li>{@link #apiSecurity}: {@code /api/**}. Stateless OAuth 2.0 resource server; the
 *       only credential is {@code Authorization: Bearer <JWT>} with an access token issued
 *       by Keycloak. The SPA gets it from GET /token; third-party clients get it from
 *       Keycloak directly (client {@code lms-cli}: authorization code + PKCE, or device
 *       flow).</li>
 *   <li>{@link #loginSecurity}: everything else. Spring Boot is an OAuth 2.0 client that
 *       lets Keycloak log the user in (backend-for-frontend); the result is a session
 *       cookie, which is only good for fetching the access token from GET /token.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder apiJwtDecoder,
                                    LmsJwtAuthenticationConverter jwtToUser) throws Exception {
        http
            .securityMatcher("/api/**")
            // Never create a session and never read the SecurityContext from one: every
            // request authenticates itself with its token.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            // The credential is an explicit header, never a cookie, so CSRF is not possible.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // Bearer JWT -> signature, expiry, issuer, audience (apiJwtDecoder)
            // -> verified email -> AuthenticatedUser + ROLE_<access> from the database.
            .oauth2ResourceServer(server -> server.jwt(jwt -> jwt
                .decoder(apiJwtDecoder)
                .jwtAuthenticationConverter(jwtToUser::convert)))
            // Missing, invalid or expired token: 401 with WWW-Authenticate: Bearer, never a
            // redirect to the login page.
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain loginSecurity(HttpSecurity http, LmsOidcUserService oidcUsers,
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
            // POST /logout: 1) invalidate the session, and with it the Keycloak tokens
            // stored there, 2) redirect to Keycloak's logout, which ends the SSO session and
            // its refresh tokens; otherwise the next login would not ask for the password.
            // Access tokens already handed out stay valid until they expire (the realm's
            // accessTokenLifespan: 5 minutes).
            .logout(logout -> logout
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

    /**
     * Validates the access tokens on /api: signature (keys from Keycloak's JWK set, fetched
     * on first use and cached), expiry, issuer and audience. The audience check makes sure
     * the token was issued for this API, not just for any client of the realm.
     */
    @Bean
    JwtDecoder apiJwtDecoder(
            @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}") String jwkSetUri,
            @Value("${lms.security.issuer-uri}") String issuerUri,
            @Value("${lms.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(apiTokenValidator(issuerUri, audience));
        return decoder;
    }

    /** The checks on an API access token besides its signature; shared with the tests. */
    static OAuth2TokenValidator<Jwt> apiTokenValidator(String issuerUri, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(audience)));
    }

    /**
     * Keeps the Keycloak tokens of a login (access and refresh token) in the HTTP session,
     * so they disappear with it at logout or session timeout.
     */
    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    /** For GET /token: the stored access token, refreshed when it is about to expire. */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                          OAuth2AuthorizedClientRepository authorizedClients) {
        var manager = new DefaultOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        return manager;
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
