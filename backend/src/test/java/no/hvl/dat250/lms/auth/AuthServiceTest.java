package no.hvl.dat250.lms.auth;

import java.time.Instant;
import java.util.List;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserService users = mock(UserService.class);
    private final AuthService auth = new AuthService(users);

    static OidcUser identity(String email, boolean verified) {
        var token = OidcIdToken.withTokenValue("test-id-token")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("sub", "keycloak-subject").claim("email", email)
                .claim("email_verified", verified).claim("given_name", "Sara")
                .claim("family_name", "Nordmann").build();
        return new DefaultOidcUser(List.of(), token);
    }

    @Test
    void delegatesVerifiedIdentityToProfileService() {
        User expected = new User("sara@example.com", "Sara", "Nordmann");
        when(users.findOrCreateOidcUser(expected.getEmail(), "Sara", "Nordmann")).thenReturn(expected);
        assertSame(expected, auth.provisionUser(identity(expected.getEmail(), true)));
    }

    @Test
    void unverifiedEmailCannotCreateOrLinkAProfile() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.provisionUser(identity("admin@example.com", false)));
        verifyNoInteractions(users);
    }

    @Test
    void missingEmailCannotCreateOrLinkAProfile() {
        var token = OidcIdToken.withTokenValue("id").claim("sub", "subject")
                .claim("email_verified", true).build();
        assertThrows(OAuth2AuthenticationException.class,
                () -> auth.provisionUser(new DefaultOidcUser(List.of(), token)));
        verifyNoInteractions(users);
    }
}
