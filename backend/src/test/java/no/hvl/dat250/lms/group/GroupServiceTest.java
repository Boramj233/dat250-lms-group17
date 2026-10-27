package no.hvl.dat250.lms.group;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    // Mock repositories and construct GroupService when its API is defined.

    @Test
    @Disabled("Not implemented yet")
    void studentCanJoinGroup() {
        // Add a student who is not already a member and verify the membership is saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCannotJoinSameGroupTwice() {
        // Given an existing membership, reject adding the same student to the same
        // group again. Verify no additional membership is saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupMustBelongToCourse() {
        // Reject creating a group without a course. Verify no group is saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupCannotReferenceUnknownCourse() {
        // Reject creating a group with a course ID that does not exist.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupCanBeCreatedForExistingCourse() {
        // Create a group for an existing course and verify the association is saved.
        fail("Not implemented yet");
    }
}
