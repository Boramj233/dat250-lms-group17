package no.hvl.dat250.lms.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {
    // Add MockMvc with the real security filters when endpoints are defined.
    // Exercise actual login/logout; do not substitute a mock authenticated user
    // in tests intended to verify authentication and its lifecycle.

    @Test
    @Disabled("Not implemented yet")
    void emailIsUniqueInTheDatabase() {
        // Add a unique constraint for emails when implementing user persistence.
        // Persist two users with the same email and flush to verify rejection.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void userHasExactlyOnePersistedCredentialPair() {
        // Verify a user requires an email and password hash. After a credential
        // change, flush/clear and reload the user: only the replacement values
        // should remain active. Previous credentials must no longer authenticate.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void anonymousUsersCannotReadProtectedInformation() {
        // Test each protected API endpoint without authentication. Verify access is
        // denied and no protected data is returned. Login and required public assets
        // must remain accessible. Account for CSRF where applicable in mutation tests.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void correctCredentialsAllowAccessToProtectedInformation() {
        // Log in with a persisted user's correct email/password, retain the
        // resulting authentication, and successfully request protected information.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void incorrectCredentialsDoNotAuthenticate() {
        // Test unknown email and wrong password separately. Verify login fails
        // and a subsequent protected request is still denied.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void logoutRevokesAccess() {
        // Log in, access protected data, then log out. Reuse the previous session or
        // credential and verify protected access is denied according to the chosen
        // authentication mechanism. Client-side deletion alone does not prove revocation.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void anonymousUsersCannotChangePasswords() {
        // Attempt a password change without authentication; verify it is rejected
        // and the original credentials still work.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void passwordChangeLogoutAndLoginWithNewPassword() {
        // Log in with the old password, change the password, and log out.
        // From a fresh unauthenticated client, verify the old password fails.
        // From another fresh client, log in with the new password and access
        // protected information. Verify the change was persisted.
        fail("Not implemented yet");
    }
}
