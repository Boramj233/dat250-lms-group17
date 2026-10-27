package no.hvl.dat250.lms.enrollment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentIntegrationTest {
    @Test
    @Disabled("Not implemented yet")
    void enrollmentApiRequiresTeacherRoleInTargetCourse() {
        // With real security filters, allow the target course's teacher to enroll
        // students. Reject students, unenrolled actors, and teachers of other courses.
        // Test each Access level with and without the required course TEACHER role;
        // global Access must not bypass this check in an existing course.
        // Use the authenticated actor's persisted role; reject attempts to gain
        // permission through a forged actor ID, role, or Access in the request.
        // Verify denied requests leave enrollments and global access unchanged.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCanBeEnrolledInMultipleCourses() {
        // Persist a student and two courses; enroll the student in both,
        // flush/clear the persistence context, then retrieve and verify both enrollments.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsDuplicateEnrollment() {
        // Verify the database rejects duplicate (user_id, course_id) pairs.
        // Add the unique constraint when implementing enrollment persistence.
        // Test identical and different roles in separate test cases; flush to trigger
        // the constraint. A constraint failure can leave the transaction unusable.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void differentRolesInDifferentCoursesCanBePersisted() {
        // Verify different roles in different courses can be persisted.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void enrollmentApiRejectsDuplicateAndConflictingEnrollments() {
        // When endpoints exist, use MockMvc to verify rejected duplicate or
        // conflicting enrollments return the agreed HTTP error and leave data unchanged.
        fail("Not implemented yet");
    }
}
