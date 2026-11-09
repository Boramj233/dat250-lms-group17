package no.hvl.dat250.lms.content;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.module.domain.Module;

public class QuizTest {

    @Test
    void quizMustAllowAtLeastOneAttempt() {

        Module module = mock(Module.class);

        assertThrows(
            IllegalArgumentException.class,
            () -> new Quiz(
                module,
                "Java Quiz",
                0,
                0
            )
        );
    }
}
