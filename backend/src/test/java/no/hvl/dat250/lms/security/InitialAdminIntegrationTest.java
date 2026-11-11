package no.hvl.dat250.lms.security;

import java.time.Instant;

import no.hvl.dat250.lms.security.login.AuthService;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Login with the application's default (invite-only) settings against the real database:
 * the configured initial admin is created as ADMIN, invited users get in with the profile
 * an admin prepared, and everyone else is refused without a profile being created.
 */
@SpringBootTest(properties = "lms.security.initial-admin-emails=boss@example.com")
@ActiveProfiles("test")
@Transactional
class InitialAdminIntegrationTest {
    @Autowired private AuthService auth;
    @Autowired private UserRepository users;

    private static OidcIdToken login(String email) {
        return OidcIdToken.withTokenValue("id-token")
                .issuer(TestTokens.ISSUER).subject("keycloak-" + email)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("email", email).claim("email_verified", true)
                .claim("given_name", "Demo").claim("family_name", "User")
                .build();
    }

    @Test
    void firstLoginOfConfiguredEmailCreatesAnAdmin() {
        auth.provisionUser(login("boss@example.com"));
        assertEquals(Access.ADMIN, users.findByEmail("boss@example.com").orElseThrow().getAccess());
    }

    @Test
    void invitedUserLogsInWithThePreparedProfile() {
        User invited = new User("kari@example.com", "Kari", "Hansen");
        invited.setAccess(Access.COURSE_MANAGER);
        users.save(invited);

        User result = auth.provisionUser(login("kari@example.com"));

        assertEquals(invited.getUserId(), result.getUserId());
        assertEquals(Access.COURSE_MANAGER, result.getAccess());
        assertEquals("Kari", result.getFirstName(), "names from Keycloak do not overwrite the profile");
    }

    @Test
    void uninvitedAccountIsRefusedAndNoProfileIsCreated() {
        var ex = assertThrows(OAuth2AuthenticationException.class,
                () -> auth.provisionUser(login("stranger@example.com")));
        assertEquals("not_invited", ex.getError().getErrorCode());
        assertTrue(users.findByEmail("stranger@example.com").isEmpty());
    }

    @Test
    void existingProfileOfConfiguredEmailKeepsItsAccess() {
        // E.g. the initial admin was demoted in the application: logging in again must not
        // make them ADMIN again.
        users.save(new User("boss@example.com", "Demo", "User"));
        auth.provisionUser(login("boss@example.com"));
        assertEquals(Access.STANDARD, users.findByEmail("boss@example.com").orElseThrow().getAccess());
    }
}
