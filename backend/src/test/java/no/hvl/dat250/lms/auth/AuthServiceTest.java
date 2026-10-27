package no.hvl.dat250.lms.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Test
    @Disabled("Not implemented yet")
    void invalidEmailIsRejected() {
        // Reject blank or malformed email addresses when creating/updating users.
        // Define email normalization rules and apply them consistently to uniqueness
        // checks and login; do not assume case/whitespace handling without a decision.
        fail("Not implemented yet");
    }

    // Mock user persistence and password verification when the service API is defined.

    @Test
    @Disabled("Not implemented yet")
    void userHasOneEmailAndOneActivePassword() {
        // Require one non-blank email and one active password hash per user.
        // Changing credentials replaces the existing value rather than adding
        // an alternative email or another active password for the same account.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void duplicateEmailIsRejected() {
        // Reject assigning an email already used by another user, both when
        // creating an account and when changing an email. Leave existing users unchanged.
        // Passwords do not need to be unique across users.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void correctEmailAndPasswordAllowLogin() {
        // Accept credentials that match an existing user and its stored password hash.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void incorrectCredentialsRejectLogin() {
        // Test wrong password, unknown email, and missing email/password separately.
        // Do not reveal whether an email exists in authentication error messages.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void authenticatedUserCanChangeOwnPassword() {
        // Verify the current password before changing it. Store a hash of the new
        // password, never plaintext, and verify the hash matches the new password.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void incorrectCurrentPasswordRejectsPasswordChange() {
        // Reject the change and leave the stored password unchanged.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void userCannotChangeAnotherUsersPassword() {
        // Use the authenticated identity to select the account being changed.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void oldPasswordIsRejectedAfterPasswordChange() {
        // Verify the old password no longer authenticates and the new one does.
        fail("Not implemented yet");
    }
}
