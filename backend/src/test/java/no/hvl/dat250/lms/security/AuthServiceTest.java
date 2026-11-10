package no.hvl.dat250.lms.security;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import no.hvl.dat250.lms.security.login.AuthService;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserService users = mock(UserService.class);
    private final AuthService auth = new AuthService(users);

    /** An id_token as Keycloak issues it for the "spring" client. */
    private static OidcIdToken idToken(Consumer<OidcIdToken.Builder> changes) {
        var builder = OidcIdToken.withTokenValue("id-token")
                .issuer("http://localhost:8081/realms/lms").subject("keycloak-subject")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("email", "sara@example.com").claim("email_verified", true)
                .claim("given_name", "Sara").claim("family_name", "Nordmann")
                .claim("name", "Sara Nordmann");
        changes.accept(builder);
        return builder.build();
    }

    @Test
    void delegatesVerifiedIdentityToProfileService() {
        User expected = new User("sara@example.com", "Sara", "Nordmann");
        when(users.findOrCreateOidcUser("sara@example.com", "Sara", "Nordmann")).thenReturn(expected);
        assertSame(expected, auth.provisionUser(idToken(token -> {})));
    }

    @Test
    void unverifiedEmailCannotCreateOrLinkAProfile() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.provisionUser(idToken(token -> token.claim("email_verified", false))));
        verifyNoInteractions(users);
    }

    @Test
    void missingEmailCannotCreateOrLinkAProfile() {
        var token = idToken(t -> t.claims(claims -> claims.remove("email")));
        assertThrows(OAuth2AuthenticationException.class, () -> auth.provisionUser(token));
        verifyNoInteractions(users);
    }

    @Test
    void fullNameIsUsedWhenGivenAndFamilyNameAreMissing() {
        auth.provisionUser(idToken(t -> t.claims(claims -> {
            claims.remove("given_name");
            claims.remove("family_name");
        })));
        verify(users).findOrCreateOidcUser("sara@example.com", "Sara Nordmann", "Sara Nordmann");
    }

    @Test
    void findUserReturnsExistingProfileWithoutCreatingOne() {
        User existing = new User("sara@example.com", "Sara", "Nordmann");
        when(users.findByEmail("sara@example.com")).thenReturn(Optional.of(existing));
        assertSame(existing, auth.findUser(idToken(token -> {})));
        verify(users, never()).findOrCreateOidcUser(any(), any(), any());
    }

    @Test
    void findUserFailsForUnknownOrUnverifiedIdentity() {
        when(users.findByEmail("sara@example.com")).thenReturn(Optional.empty());
        assertThrows(OAuth2AuthenticationException.class, () -> auth.findUser(idToken(token -> {})));
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.findUser(idToken(token -> token.claim("email_verified", false))));
    }
}
