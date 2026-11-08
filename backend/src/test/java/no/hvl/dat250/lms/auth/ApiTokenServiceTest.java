package no.hvl.dat250.lms.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiTokenServiceTest {
    private final UserService users = mock(UserService.class);
    private final Clock clock = mock(Clock.class);
    private final User user = mock(User.class);
    private final Instant now = Instant.parse("2026-11-04T09:00:00Z");
    private ApiTokenService tokens;

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(now);
        when(user.getUserId()).thenReturn(42L);
        tokens = new ApiTokenService(users, clock, Duration.ofMinutes(15));
    }

    @Test
    void tokensAreRandomAndResolveToTheStoredUser() {
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(user.getEmail()).thenReturn("sara@example.com");
        var first = tokens.issue(user);
        var second = tokens.issue(user);
        assertNotEquals(first.accessToken(), second.accessToken());
        assertEquals(43, first.accessToken().length());
        assertEquals(now.plusSeconds(900), first.expiresAt());
        assertSame(user, tokens.introspect(first.accessToken()).getAttribute("user"));
    }

    @Test
    void tokenExpiresAtItsExactExpiryTime() {
        var token = tokens.issue(user);
        when(clock.instant()).thenReturn(token.expiresAt());
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(token.accessToken()));
        verifyNoInteractions(users);
    }

    @Test
    void unknownAndModifiedTokensAreRejected() {
        var token = tokens.issue(user);
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect("unknown"));
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(token.accessToken() + "x"));
    }

    @Test
    void revocationImmediatelyRejectsAnOtherwiseValidToken() {
        var token = tokens.issue(user);
        tokens.revoke(token.accessToken());
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(token.accessToken()));
    }

    @Test
    void deletingTheLocalUserInvalidatesTheirToken() {
        var token = tokens.issue(user);
        when(users.findById(42L)).thenReturn(Optional.empty());
        assertThrows(BadOpaqueTokenException.class, () -> tokens.introspect(token.accessToken()));
    }
}
