package no.hvl.dat250.lms.module;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModuleIntegrationTest {
    @Test
    @Disabled("Not implemented yet")
    void moduleOrderSurvivesPersistenceAndReordering() {
        // Save and reorder modules, flush/clear, and verify API/repository results
        // preserve the intended order within each course.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void modulePrerequisitesAreEnforcedThroughApi() {
        // If dependencies are implemented, test student access before and after
        // completion of all prerequisites using persisted interactions.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void moduleCourseAssociationCanBePersisted() {
        // Persist a course and module, flush/clear, and verify the course association.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsModuleWithoutCourse() {
        // Make the course foreign key non-nullable; verify a missing course is
        // rejected on flush.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsModuleWithUnknownCourse() {
        // Verify the course foreign key rejects a nonexistent course on flush.
        // Use a separate test for each constraint failure.
        fail("Not implemented yet");
    }
}
