package no.hvl.dat250.lms.group;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupIntegrationTest {
    @Test
    @Disabled("Not implemented yet")
    void groupAndMembershipCanBePersisted() {
        // Persist a course, group, student, and membership. Flush/clear, then verify
        // the group references the course and contains the student exactly once.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsDuplicateGroupMembership() {
        // Add a unique constraint on (student_id, group_id), or the equivalent
        // membership keys. Attempt a duplicate insert and flush to verify rejection.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsGroupWithoutCourse() {
        // Make the group's course foreign key non-nullable. Persist a group without
        // a course and flush to verify rejection.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsGroupWithUnknownCourse() {
        // Verify the course foreign key rejects a reference to a nonexistent course.
        // Use a separate test for each constraint failure; a failed flush can leave
        // the transaction unusable.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupApiRejectsInvalidMembershipAndCourse() {
        // When endpoints exist, use MockMvc to verify duplicate membership
        // and missing/unknown course requests return the agreed HTTP errors without
        // changing existing groups or memberships.
        fail("Not implemented yet");
    }
}
