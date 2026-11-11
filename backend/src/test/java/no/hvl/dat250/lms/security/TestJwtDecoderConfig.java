package no.hvl.dat250.lms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Lets the API chain trust the test signing key from {@link TestTokens} instead of
 * fetching Keycloak's JWK set, which does not run during tests.
 *
 * <p>A plain {@code @Configuration} in the test sources, so the component scan of every
 * {@code @SpringBootTest} picks it up (slice tests such as {@code @WebMvcTest} do not).
 * {@code @Primary} wins over {@link SecurityConfig#apiJwtDecoder}; the claim checks are
 * the production ones from {@link SecurityConfig#apiTokenValidator}.
 */
@Configuration
class TestJwtDecoderConfig {

    @Bean
    @Primary
    JwtDecoder testApiJwtDecoder(@Value("${lms.security.issuer-uri}") String issuerUri,
                                 @Value("${lms.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestTokens.publicKey()).build();
        decoder.setJwtValidator(SecurityConfig.apiTokenValidator(issuerUri, audience));
        return decoder;
    }
}
