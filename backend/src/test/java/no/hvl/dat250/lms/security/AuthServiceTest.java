package no.hvl.dat250.lms.security;

import java.time.Instant;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserService users = mock(UserService.class);
    private final AuthService auth = new AuthService(users);

    private static Jwt token(String email, boolean verified) {
        return Jwt.withTokenValue("test-access-token").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("sub", "keycloak-subject").claim("email", email)
                .claim("email_verified", verified).claim("given_name", "Sara")
                .claim("family_name", "Nordmann").build();
    }

    @Test
    void delegatesVerifiedIdentityToProfileService() {
        User expected = new User("sara@example.com", "Sara", "Nordmann");
        when(users.findOrCreateOidcUser(expected.getEmail(), "Sara", "Nordmann")).thenReturn(expected);
        assertSame(expected, auth.provisionUser(token(expected.getEmail(), true)));
    }

    @Test
    void unverifiedEmailCannotCreateOrLinkAProfile() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.provisionUser(token("admin@example.com", false)));
        verifyNoInteractions(users);
    }

    @Test
    void missingEmailCannotCreateOrLinkAProfile() {
        var token = Jwt.withTokenValue("t").header("alg", "RS256").claim("sub", "subject")
                .claim("email_verified", true).build();
        assertThrows(OAuth2AuthenticationException.class, () -> auth.provisionUser(token));
        verifyNoInteractions(users);
    }

    @Test
    void fullNameIsUsedWhenGivenAndFamilyNameAreMissing() {
        var token = Jwt.withTokenValue("t").header("alg", "RS256").claim("sub", "subject")
                .claim("email", "sara@example.com").claim("email_verified", true)
                .claim("name", "Sara Nordmann").build();
        auth.provisionUser(token);
        verify(users).findOrCreateOidcUser("sara@example.com", "Sara Nordmann", "Sara Nordmann");
    }
}
