package no.hvl.dat250.lms.content;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {
    @Test
    @Disabled("Not implemented yet")
    void contentMustBelongToExistingModule() {
        // Reject missing/unknown modules; accept content attached to an existing module.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void courseTeacherCanCreateAndEditSupportedContentTypes() {
        // Cover static pages (including multimedia references), quizzes, coding
        // exercises, and hand-in assignments. Reject students and other-course teachers.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void contentOrderIsScopedToModule() {
        // Test insertion/reordering of items without changing another module's order.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentsOnlySeeContentWithinVisibilityWindow() {
        // Use a fixed/injected clock. Test before opening, during visibility, and
        // after closing, including exact boundaries according to the chosen policy.
        // Missing time bounds should not unnecessarily hide otherwise accessible content.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void quizAnswersAreGradedCorrectly() {
        // Test correct, incorrect, and incomplete answers for each supported question
        // type, with the chosen scoring policy and configured hints for wrong answers.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void quizAttemptLimitIsEnforcedPerStudentAndQuiz() {
        // Allow attempts up to the configured limit; reject the next attempt.
        // Another student's attempts or another quiz must not consume that limit.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void codingExerciseIsGradedUsingConfiguredTests() {
        // When implemented, verify grading outcomes with a mocked execution component.
        // Cover passing, failing, and execution-error/timeout results; hidden test
        // details must not leak to students. Keep actual runner tests separate.
        fail("Not implemented yet");
    }
}
