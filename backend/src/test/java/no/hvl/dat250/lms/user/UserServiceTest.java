package no.hvl.dat250.lms.user;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    // Mock repositories and construct UserService when its API is defined.
    // Authentication and password lifecycle scenarios belong in the auth tests.

    @Test
    @Disabled("Not implemented yet")
    void userCanBeCreatedWithValidDetails() {
        // Accept a valid email and required user details; verify the user is saved.
        // Define required profile fields before implementing their validation.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void invalidUserDetailsAreRejected() {
        // Reject blank/malformed email and missing required details separately.
        // Reject an email already assigned to another user; save nothing on failure.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void existingUserCanBeRetrieved() {
        // Retrieve the requested user by ID and verify the returned profile data.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void unknownUserIsHandledExplicitly() {
        // Request an unknown user ID and verify the agreed not-found outcome.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void profileChangesDoNotGrantGlobalAccess() {
        // An ordinary profile update must not allow elevation to COURSE_MANAGER
        // or ADMIN. Keep global access changes separate from editable profile data.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void globalAccessChangesRequireExplicitAuthorization() {
        // Define who may assign STANDARD, COURSE_MANAGER, and ADMIN, then test
        // permitted and rejected actors. Do not infer this policy from course roles.
        // Rejected changes must leave the user's current Access unchanged.
        fail("Not implemented yet");
    }
}
