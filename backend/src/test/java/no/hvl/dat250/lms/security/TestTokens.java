package no.hvl.dat250.lms.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test tokens for MockMvc tests. Tests replace the real {@link JwtDecoder} (which needs
 * Keycloak's keys) with a mock; everything after decoding runs for real: the
 * resource-server filter, {@link LmsJwtAuthenticationConverter}, the database lookup,
 * {@code @PreAuthorize} and the services. Signature/issuer checks are tested
 * separately in {@code JwtDecoderConfigTest}.
 */
public final class TestTokens {
    // Bearer tokens may only contain base64url-like characters, so the email is encoded.
    private static final String VERIFIED = "verified.";
    private static final String UNVERIFIED = "unverified.";

    private TestTokens() {}

    /** Makes the mocked decoder accept tokens from {@link #bearer} and reject anything else. */
    public static void install(JwtDecoder decoder) {
        when(decoder.decode(anyString())).thenAnswer(call -> decode(call.getArgument(0)));
    }

    /** Authorization header for a Keycloak user with this verified email. */
    public static String bearer(String email) {
        return "Bearer " + VERIFIED + encode(email);
    }

    /** Authorization header for a Keycloak user whose email is not verified. */
    public static String bearerUnverified(String email) {
        return "Bearer " + UNVERIFIED + encode(email);
    }

    /** The claims Keycloak puts in an access token for the LMS frontend client. */
    public static Jwt jwt(String email, boolean emailVerified) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token-for-" + email)
                .header("alg", "RS256")
                .issuer("http://localhost:8081/realms/lms")
                .subject("keycloak-id-" + email)
                .claim("azp", "lms-frontend")
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }

    private static Jwt decode(String token) {
        if (token.startsWith(VERIFIED)) return jwt(decodeEmail(token.substring(VERIFIED.length())), true);
        if (token.startsWith(UNVERIFIED)) return jwt(decodeEmail(token.substring(UNVERIFIED.length())), false);
        throw new BadJwtException("Invalid test token");
    }

    private static String encode(String email) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(email.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeEmail(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
