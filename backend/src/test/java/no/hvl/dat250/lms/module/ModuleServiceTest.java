package no.hvl.dat250.lms.module;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {
    // Mock repositories and construct ModuleService when its API is defined.

    @Test
    @Disabled("Not implemented yet")
    void modulesKeepTheirOrderWithinCourse() {
        // Verify insertion and reordering produce the expected order in one course
        // without affecting modules belonging to another course.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void dependentModuleRequiresAllPrerequisitesToBeCompleted() {
        // If module dependencies are implemented, reject student access when any
        // prerequisite is incomplete and allow it once all are completed.
        // Define completion for pages, quizzes, and assignments before testing it.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void onlyTeachersOfModulesCourseCanModifyModule() {
        // Reject students and teachers of other courses; allow this course's teacher.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void moduleCanBeCreatedForExistingCourse() {
        // Create a module for an existing course and verify the association is saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void moduleMustBelongToCourse() {
        // Reject creating a module without a course; verify no module is saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void moduleCannotReferenceUnknownCourse() {
        // Reject a nonexistent course ID; verify no module is saved.
        fail("Not implemented yet");
    }
}
