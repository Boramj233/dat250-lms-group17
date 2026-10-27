package no.hvl.dat250.lms.course;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    // Add @Mock repositories and construct the service when its API is defined.
    // Implement one scenario per business rule, using Arrange, Act, Assert.

    @Test
    @Disabled("Not implemented yet")
    void courseCanBeCreatedWithTitleDescriptionAndSemester() {
        // Accept valid details and verify all required fields are saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void missingRequiredCourseDetailsAreRejected() {
        // Test missing/blank title, description, and missing semester separately.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void courseCreationUsesUserAccess() {
        // Test STANDARD, COURSE_MANAGER, and ADMIN separately against the agreed
        // permission policy. Define the allowed levels and any management scope.
        // A TEACHER enrollment alone must not grant course-creation permission.
        // Verify rejected requests save nothing.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void initialCourseRoleAssignmentUsesUserAccess() {
        // An authorized creator can assign initial course roles during course creation.
        // Verify this permission cannot be reused to enroll students in an existing
        // course without being a TEACHER there. User.Access remains unchanged.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void onlyCourseTeachersCanAdministerCourseContent() {
        // Accept administration by a teacher assigned to this course; reject students
        // and users who are teachers only in another course. Keep course-content
        // permissions separate from Access rules for creation and initial role assignment.
        fail("Not implemented yet");
    }
}
