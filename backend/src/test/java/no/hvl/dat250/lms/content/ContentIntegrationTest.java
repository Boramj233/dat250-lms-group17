package no.hvl.dat250.lms.content;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentIntegrationTest {
    @Test
    @Disabled("Not implemented yet")
    void supportedContentTypesAndOrderSurvivePersistence() {
        // Persist each supported type and its module association, reorder items,
        // flush/clear, and verify fields and ordering through the API.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void contentAccessUsesCourseRoleAndVisibilityWindow() {
        // With real security filters and a controlled clock, deny student access
        // outside the window and access by users not enrolled in the course.
        // Verify list and direct-ID endpoints do not expose hidden content.
        // Reject content changes by students and teachers of other courses.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void quizAttemptsAndResultsArePersisted() {
        // Submit answers, verify grading/results after reload, and enforce the
        // configured attempt limit through the API. Never expose answer keys or
        // another student's private attempts in student responses.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentInteractionsRecordProgress() {
        // Interact with content, persist/reload, and verify progress belongs to
        // the correct student and content item, supporting module completion rules.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void externalStorageAndDomainEventsWorkWhenImplemented() {
        // Add focused integration tests for the selected NoSQL/object storage and
        // messaging system: save/retrieve submitted files, deny unauthorized file
        // access, and verify events for interactions/submissions (including late ones).
        // Use isolated test resources; do not rely on production services.
        fail("Not implemented yet");
    }
}
