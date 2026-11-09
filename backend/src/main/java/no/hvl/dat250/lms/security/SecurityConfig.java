package no.hvl.dat250.lms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Boot is an OAuth 2.0 client: Keycloak performs the login and the backend keeps
 * a session for the browser.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder jwtDecoder,
                                    AuthService authService) throws Exception {
        http
            .securityMatcher("/api/**")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            // Authentication is an explicit Bearer header, never a cookie, so CSRF is not possible.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // Missing/invalid token -> 401 with WWW-Authenticate: Bearer (no login redirect).
            .oauth2ResourceServer(server -> server.jwt(jwt -> jwt
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(new LmsJwtAuthenticationConverter(authService))));
        return http.build();
    }

    /** Login with Keycloak (authorization code flow); the result is a session cookie. */
    @Bean
    @Order(2)
    SecurityFilterChain loginSecurity(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error", "/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                .anyRequest().denyAll())
            .requestCache(cache -> cache.disable())
            .oauth2Login(login -> login
                // Without our own login page Spring would render a default one at /login.
                .loginPage("/oauth2/authorization/keycloak")
                // Back to the SPA, on the address the browser used (Vite or nginx).
                .defaultSuccessUrl("/", true)
                .failureUrl("/?login_error"));
        return http.build();
    }

    /**
     * Validates the id_token Keycloak returns from the code exchange: signature (keys from
     * the internal JWK set URI), audience, expiry, and the issuer. The endpoints are
     * configured one by one (public and internal URL differ), so Spring has no issuer to
     * compare with; Keycloak writes its public URL into the iss claim.
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
     * Keys are fetched from Keycloak's internal address (keycloak:8080 in Docker), while
     * the issuer is checked against the public address the browser used to log in
     * (localhost:8081). Keycloak puts that public URL in the token's iss claim.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${lms.security.jwk-set-uri}") String jwkSetUri,
                          @Value("${lms.security.issuer-uri}") String issuerUri,
                          @Value("${lms.security.client-id}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(jwtValidator(issuerUri, clientId));
        return decoder;
    }

    /**
     * Signature is checked by the decoder; this checks the claims: expiry/not-before
     * (default validators), issuer, and that the token was issued to our frontend client.
     */
    public static OAuth2TokenValidator<Jwt> jwtValidator(String issuerUri, String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new JwtClaimValidator<String>("azp", clientId::equals));
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
