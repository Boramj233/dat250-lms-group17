package no.hvl.dat250.lms.config;

import no.hvl.dat250.lms.auth.AuthService;
import no.hvl.dat250.lms.auth.LmsJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The backend is an OAuth2 resource server: every API call carries a Keycloak access
 * token (JWT) in the Authorization header. There is no login, session or cookie here.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder jwtDecoder,
                                    AuthService authService) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            // Authentication is an explicit Bearer header, never a cookie, so CSRF is not possible.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                // Spring's error page, used when rendering error responses for API calls.
                .requestMatchers("/error").permitAll()
                // The backend only serves the API; everything else is closed.
                .anyRequest().denyAll())
            // Missing/invalid token -> 401 with WWW-Authenticate: Bearer (no login redirect).
            .oauth2ResourceServer(server -> server.jwt(jwt -> jwt
                .decoder(jwtDecoder)
                // Validated JWT -> AuthenticatedUser(userId) + ROLE_<access> from the database.
                .jwtAuthenticationConverter(new LmsJwtAuthenticationConverter(authService))));
        return http.build();
    }

    /**
     * Keys are fetched from Keycloak's internal address (keycloak:8080 in Docker), while
     * the issuer is checked against the public address the browser used to log in
     * (localhost:8081). Keycloak puts that public URL in the token's iss claim.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${lms.auth.jwk-set-uri}") String jwkSetUri,
                          @Value("${lms.auth.issuer-uri}") String issuerUri,
                          @Value("${lms.auth.client-id}") String clientId) {
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
