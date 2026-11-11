package no.hvl.dat250.lms.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * API credentials for MockMvc tests.
 *
 * <p>Tests play Keycloak: they sign access tokens with a key pair generated for the test
 * run, and {@link TestJwtDecoderConfig} makes the API chain trust that key instead of
 * Keycloak's JWK set. Everything else (expiry, issuer and audience checks, the database
 * lookup, {@code @PreAuthorize}) is the application's real code.
 */
public final class TestTokens {
    /** The issuer the application expects (lms.security.issuer-uri). */
    public static final String ISSUER = "http://localhost:8081/realms/lms";
    /** The audience the application expects (lms.security.audience). */
    public static final String AUDIENCE = "lms-api";

    private static final KeyPair KEYS = newKeyPair();
    private static final JwtEncoder ENCODER = encoder(KEYS);

    private TestTokens() {}

    /** Authorization header with a valid access token for this user, as Keycloak issues it. */
    public static String bearer(User user) {
        return "Bearer " + accessToken(user.getEmail());
    }

    /** A valid access token for this (verified) email. */
    public static String accessToken(String email) {
        return accessToken(claims -> claims.claim("email", email));
    }

    /**
     * An access token signed with the trusted test key. Starts from a valid token (issuer,
     * audience, verified email, five minutes' lifetime) that the customizer can change.
     */
    public static String accessToken(Consumer<JwtClaimsSet.Builder> customizer) {
        return sign(ENCODER, customizer);
    }

    /** A token that is valid in every respect except that the API does not know its key. */
    public static String accessTokenWithUnknownKey(String email) {
        return sign(encoder(newKeyPair()), claims -> claims.claim("email", email));
    }

    /** The public half of the trusted test key, for {@link TestJwtDecoderConfig}. */
    static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEYS.getPublic();
    }

    /**
     * For controller slice tests ({@link WithApiSecurity}), where the JWT handling is
     * mocked: the request is authenticated as if the API chain had accepted a token for
     * this principal. The filter chain, CSRF/session settings and method security still run.
     */
    public static RequestPostProcessor apiUser(long userId, Access access) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + access.name()));
        var principal = new AuthenticatedUser(userId, authorities);
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-token",
                Instant.now(), Instant.now().plusSeconds(300));
        return authentication(new BearerTokenAuthentication(principal, token, authorities));
    }

    /** A STANDARD user, the default caller in controller slice tests. */
    public static RequestPostProcessor apiUser() {
        return apiUser(1L, Access.STANDARD);
    }

    /** A session as the login chain stores it after a successful Keycloak login. */
    public static MockHttpSession loginSession(String email) {
        Instant issued = Instant.now();
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer(ISSUER).subject("keycloak-id")
                .claim("email", email).claim("email_verified", true)
                .issuedAt(issued).expiresAt(issued.plusSeconds(300)).build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
        var authentication = new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }

    private static String sign(JwtEncoder encoder, Consumer<JwtClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject("keycloak-id")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("email_verified", true);
        customizer.accept(claims);
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static JwtEncoder encoder(KeyPair keys) {
        RSAKey key = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
                .privateKey((RSAPrivateKey) keys.getPrivate())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
