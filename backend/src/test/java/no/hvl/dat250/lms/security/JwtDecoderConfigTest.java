package no.hvl.dat250.lms.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation with real signed JWTs. The production decoder loads Keycloak's public key
 * from the JWK set URI; here the key is local, but the claim validator is the same one.
 */
class JwtDecoderConfigTest {
    private static final String ISSUER = "http://localhost:8081/realms/lms";
    private static final String CLIENT_ID = "lms-frontend";

    private final KeyPair keycloakKey = rsaKeyPair();
    private final JwtDecoder decoder = decoderTrusting(keycloakKey);

    private static KeyPair rsaKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static JwtDecoder decoderTrusting(KeyPair key) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) key.getPublic()).build();
        decoder.setJwtValidator(SecurityConfig.jwtValidator(ISSUER, CLIENT_ID));
        return decoder;
    }

    private static String sign(KeyPair key, Consumer<JWTClaimsSet.Builder> changes) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER).subject("keycloak-id").claim("azp", CLIENT_ID)
                .claim("email", "student1@example.com").claim("email_verified", true)
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)));
        changes.accept(claims);
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner(key.getPrivate()));
        return jwt.serialize();
    }

    @Test
    void acceptsTokenFromKeycloakForTheFrontendClient() throws Exception {
        var jwt = decoder.decode(sign(keycloakKey, claims -> {}));
        assertEquals("student1@example.com", jwt.getClaimAsString("email"));
    }

    @Test
    void rejectsTokenWithInternalDockerIssuer() throws Exception {
        // The browser logs in via localhost:8081, so that is the only accepted issuer.
        String token = sign(keycloakKey, claims -> claims.issuer("http://keycloak:8080/realms/lms"));
        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsTokenIssuedToAnotherClient() throws Exception {
        String token = sign(keycloakKey, claims -> claims.claim("azp", "some-other-client"));
        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        String token = sign(keycloakKey, claims -> claims
                .issueTime(Date.from(past)).expirationTime(Date.from(past.plusSeconds(300))));
        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() throws Exception {
        String forged = sign(rsaKeyPair(), claims -> {});
        assertThrows(BadJwtException.class, () -> decoder.decode(forged));
    }
}
