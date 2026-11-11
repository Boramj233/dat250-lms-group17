package no.hvl.dat250.lms.security;

import java.time.Instant;

import no.hvl.dat250.lms.security.login.AuthService;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap admin end to end: the first login of a configured email creates the profile
 * with ADMIN in the database; existing profiles and other emails are untouched.
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
    void firstLoginOfOtherEmailCreatesAStandardUser() {
        auth.provisionUser(login("sara@example.com"));
        assertEquals(Access.STANDARD, users.findByEmail("sara@example.com").orElseThrow().getAccess());
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
