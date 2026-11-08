package no.hvl.dat250.lms.auth;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.hvl.dat250.lms.auth.dto.TokenResponse;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Service;

/** Application tokens are independent of the Keycloak tokens used during login. */
@Service
public class ApiTokenService implements OpaqueTokenIntrospector {
    private record StoredToken(Long userId, Instant expiresAt) {}

    private final ConcurrentHashMap<String, StoredToken> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserService users;
    private final Clock clock;
    private final Duration lifetime;

    public ApiTokenService(UserService users, Clock clock,
            @Value("${lms.auth.token-lifetime:PT15M}") Duration lifetime) {
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }
        this.users = users;
        this.clock = clock;
        this.lifetime = lifetime;
    }

    public TokenResponse issue(User user) {
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("A persisted user is required");
        }
        Instant now = clock.instant();
        tokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(lifetime);
        tokens.put(hash(token), new StoredToken(user.getUserId(), expiresAt));
        return new TokenResponse(token, "Bearer", expiresAt);
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        String key = hash(token);
        StoredToken stored = tokens.get(key);
        if (stored == null || !stored.expiresAt().isAfter(clock.instant())) {
            tokens.remove(key);
            throw new BadOpaqueTokenException("Invalid or expired API token");
        }
        User user = users.findById(stored.userId()).orElseThrow(() -> {
            tokens.remove(key);
            return new BadOpaqueTokenException("Invalid API token");
        });
        // Reload the profile on every request so stored access changes take effect immediately.
        return new DefaultOAuth2AuthenticatedPrincipal(user.getEmail(),
                Map.of("sub", user.getEmail(), "user", user), List.of());
    }

    public void revoke(String token) {
        tokens.remove(hash(token));
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
