package no.hvl.dat250.lms.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Service;

/**
 * Issues and checks the backend's own API tokens.
 *
 * <p>A token is 32 random bytes from {@link SecureRandom}, base64url-encoded. It carries
 * no information itself (opaque); it only identifies an entry in this service. Only the
 * SHA-256 hash of the token is stored, together with the user ID and the expiry time, so
 * a dump of the map cannot be used to call the API. Tokens live in memory: a restart of
 * the backend logs every API client out, and the SPA simply fetches a new token.
 *
 * <p>As {@link OpaqueTokenIntrospector}, it is called by the API filter chain for every
 * request with {@code Authorization: Bearer <token>}.
 */
@Service
public class TokenService implements OpaqueTokenIntrospector {
    private static final int TOKEN_BYTES = 32;

    private record StoredToken(Long userId, Instant expiresAt) {}

    private final ConcurrentHashMap<String, StoredToken> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserService users;
    private final Clock clock;
    private final Duration lifetime;

    public TokenService(UserService users, Clock clock,
                        @Value("${lms.security.token-lifetime:PT15M}") Duration lifetime) {
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }
        this.users = users;
        this.clock = clock;
        this.lifetime = lifetime;
    }

    /** Creates a new token for a stored user. The plain token is returned only here. */
    public TokenResponse issue(User user) {
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("A persisted user is required");
        }
        Instant now = clock.instant();
        // Expired tokens can never be used again; drop them so the map does not grow.
        tokens.values().removeIf(stored -> !stored.expiresAt().isAfter(now));

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(lifetime);
        tokens.put(hash(token), new StoredToken(user.getUserId(), expiresAt));
        return new TokenResponse(token, "Bearer", expiresAt);
    }

    /**
     * Checks a token from the Authorization header. Unknown, expired or orphaned tokens
     * throw {@link BadOpaqueTokenException}, which the API chain answers with 401.
     */
    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        String key = hash(token);
        StoredToken stored = tokens.get(key);
        if (stored == null) {
            throw new BadOpaqueTokenException("Unknown token");
        }
        if (!stored.expiresAt().isAfter(clock.instant())) {
            tokens.remove(key);
            throw new BadOpaqueTokenException("Expired token");
        }
        // Read access from the database on every request, so changes apply immediately.
        User user = users.findById(stored.userId()).orElseThrow(() -> {
            tokens.remove(key);
            return new BadOpaqueTokenException("User no longer exists");
        });
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getAccess().name());
        return new AuthenticatedUser(user.getUserId(), List.of(authority));
    }

    /**
     * Revokes every token of this user (used on logout). The tokens are opaque and only
     * valid while their entry is in the map, so removing the entries is enough.
     *
     * @return the number of tokens revoked
     */
    public int revokeAllFor(Long userId) {
        int before = tokens.size();
        tokens.values().removeIf(stored -> stored.userId().equals(userId));
        return before - tokens.size();
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
