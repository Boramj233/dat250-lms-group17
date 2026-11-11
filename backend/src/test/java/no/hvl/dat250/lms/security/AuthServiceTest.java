package no.hvl.dat250.lms.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import no.hvl.dat250.lms.security.login.AuthService;
import no.hvl.dat250.lms.security.login.InitialAdmins;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserService users = mock(UserService.class);
    // boss@example.com is the configured initial admin (mixed case and blanks are ignored).
    private final AuthService auth = new AuthService(users,
            new InitialAdmins(List.of(" Boss@Example.com ", "")));

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
    void delegatesVerifiedIdentityToProfileServiceWithStandardAccess() {
        User expected = new User("sara@example.com", "Sara", "Nordmann");
        when(users.findOrCreateOidcUser("sara@example.com", "Sara", "Nordmann", Access.STANDARD))
                .thenReturn(expected);
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
        verify(users).findOrCreateOidcUser("sara@example.com", "Sara Nordmann", "Sara Nordmann", Access.STANDARD);
    }

    @Test
    void configuredInitialAdminIsCreatedWithAdminAccess() {
        auth.provisionUser(idToken(token -> token.claim("email", "boss@example.com")));
        // UserService applies the initial access only when it creates the profile.
        verify(users).findOrCreateOidcUser("boss@example.com", "Sara", "Nordmann", Access.ADMIN);
    }

    @Test
    void unverifiedEmailOfConfiguredInitialAdminCannotLogIn() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.provisionUser(idToken(token -> token
                        .claim("email", "boss@example.com").claim("email_verified", false))));
        verifyNoInteractions(users);
    }

    @Test
    void initialAdminsMatchIgnoringCaseAndNoOneElse() {
        var admins = new InitialAdmins(List.of(" Boss@Example.com ", ""));
        assertEquals(Access.ADMIN, admins.initialAccessFor("boss@example.com"));
        assertEquals(Access.ADMIN, admins.initialAccessFor("BOSS@example.COM"));
        assertEquals(Access.STANDARD, admins.initialAccessFor("sara@example.com"));
        assertEquals(Access.STANDARD, admins.initialAccessFor(""));
        assertEquals(Access.STANDARD, new InitialAdmins(List.of()).initialAccessFor("boss@example.com"));
    }

    @Test
    void findUserReturnsExistingProfileWithoutCreatingOne() {
        User existing = new User("sara@example.com", "Sara", "Nordmann");
        when(users.findByEmail("sara@example.com")).thenReturn(Optional.of(existing));
        assertSame(existing, auth.findUser(idToken(token -> {})));
        verify(users, never()).findOrCreateOidcUser(any(), any(), any(), any());
    }

    @Test
    void findUserFailsForUnknownOrUnverifiedIdentity() {
        when(users.findByEmail("sara@example.com")).thenReturn(Optional.empty());
        assertThrows(OAuth2AuthenticationException.class, () -> auth.findUser(idToken(token -> {})));
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.findUser(idToken(token -> token.claim("email_verified", false))));
    }
}
