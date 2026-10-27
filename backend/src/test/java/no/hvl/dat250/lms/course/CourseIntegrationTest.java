package no.hvl.dat250.lms.course;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseIntegrationTest {
    // Implement the disabled scenarios when the endpoints are implemented.
    // For HTTP tests, add @AutoConfigureMockMvc and inject MockMvc.
    // Transactions roll back after each test when using same-thread calls/MockMvc.

    @Test
    @Disabled("Not implemented yet")
    void courseCanBeCreatedAndRetrievedThroughApi() {
        // Create a course, flush/clear, retrieve it, and verify title, description,
        // and semester. Invalid required fields must be rejected without saving.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void courseCreationApiEnforcesUserAccess() {
        // With real security filters, test STANDARD, COURSE_MANAGER, and ADMIN
        // against the agreed creation policy. Use the authenticated user's persisted
        // Access, never an Access value supplied in the request. Denied requests
        // must not create courses or enrollments.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void authorizedCreatorCanAssignInitialCourseRoles() {
        // Create a course and assign its initial roles using the creator's Access.
        // Flush/clear and verify the course and roles were persisted. A rejected
        // creation/initial-role request must leave no partially created data.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void courseContentAdministrationUsesCourseScopedRoles() {
        // Verify a teacher of course A can edit its content but cannot edit course B
        // solely due to their role in A. Reject student edits, including manipulated
        // resource IDs. Access for creating courses must not itself grant course
        // teaching permissions; enrollment is covered by EnrollmentIntegrationTest.
        fail("Not implemented yet");
    }
}
