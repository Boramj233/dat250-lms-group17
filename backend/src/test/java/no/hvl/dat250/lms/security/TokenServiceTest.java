package no.hvl.dat250.lms.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenServiceTest {
    private static final Instant START = Instant.parse("2026-11-09T12:00:00Z");

    private final UserService users = mock(UserService.class);
    private Instant now = START;
    private final Clock clock = new Clock() {
        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    };
    private final TokenService tokens = new TokenService(users, clock, Duration.ofMinutes(15));

    private User storedUser(long id, Access access) {
        User user = new User("user" + id + "@example.com", "Test", "User");
        user.setAccess(access);
        ReflectionTestUtils.setField(user, "userId", id);
        when(users.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void issuesRandomBase64UrlTokensWithLifetimeFromConfiguration() {
        User user = storedUser(7, Access.STANDARD);
        TokenResponse first = tokens.issue(user);
        TokenResponse second = tokens.issue(user);
        assertTrue(first.accessToken().matches("[A-Za-z0-9_-]{43}"), "32 bytes, base64url");
        assertNotEquals(first.accessToken(), second.accessToken());
        assertEquals("Bearer", first.tokenType());
        assertEquals(START.plus(Duration.ofMinutes(15)), first.expiresAt());
    }

    @Test
    void introspectionReturnsTheUserWithRoleFromTheDatabase() {
        User user = storedUser(7, Access.COURSE_MANAGER);
        var principal = (AuthenticatedUser) tokens.introspect(tokens.issue(user).accessToken());
        assertEquals(7L, principal.userId());
        assertEquals("7", principal.getName());
        assertEquals(java.util.List.of("ROLE_COURSE_MANAGER"),
                principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void storesOnlyTheSha256HashOfTheToken() throws Exception {
        String token = tokens.issue(storedUser(7, Access.STANDARD)).accessToken();
        @SuppressWarnings("unchecked")
        Map<String, ?> stored = (Map<String, ?>) ReflectionTestUtils.getField(tokens, "tokens");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        assertEquals(java.util.Set.of(hash), stored.keySet());
        assertFalse(stored.toString().contains(token));
    }

    @Test
    void unknownTokenIsRejected() {
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect("unknown"));
    }

    @Test
    void tokenExpiresAfterItsLifetime() {
        String token = tokens.issue(storedUser(7, Access.STANDARD)).accessToken();
        now = START.plus(Duration.ofMinutes(15)).minusMillis(1);
        assertNotNull(tokens.introspect(token));
        now = START.plus(Duration.ofMinutes(15));
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(token));
    }

    @Test
    void tokenOfDeletedUserIsRejected() {
        String token = tokens.issue(storedUser(7, Access.STANDARD)).accessToken();
        when(users.findById(7L)).thenReturn(Optional.empty());
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(token));
    }

    @Test
    void revokeAllForRemovesOnlyThatUsersTokens() {
        User sara = storedUser(7, Access.STANDARD);
        User ola = storedUser(8, Access.STANDARD);
        String saraBrowser = tokens.issue(sara).accessToken();
        String saraOtherTab = tokens.issue(sara).accessToken();
        String olaToken = tokens.issue(ola).accessToken();

        assertEquals(2, tokens.revokeAllFor(7L));

        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(saraBrowser));
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(saraOtherTab));
        assertEquals(8L, ((AuthenticatedUser) tokens.introspect(olaToken)).userId());
        assertEquals(0, tokens.revokeAllFor(7L));
    }

    @Test
    void lifetimeMustBePositiveAndUserMustBeStored() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService(users, clock, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> tokens.issue(new User("new@example.com", "New", "User")));
    }
}
