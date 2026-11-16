
package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.Set;

import no.hvl.dat250.lms.content.domain.ChoiceOption;
import no.hvl.dat250.lms.content.service.QuizGrader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizGraderTest {

    private final QuizGrader grader = new QuizGrader();

    @Test
    void textMatchingTrimsEdgesAndIgnoresCaseButPreservesInternalSpaces() {
        Set<String> accepted = Set.of(" Java ", "Java language");
        assertTrue(grader.isTextCorrect(accepted, "  JAVA  "));
        assertTrue(grader.isTextCorrect(accepted, "java LANGUAGE"));
        assertFalse(grader.isTextCorrect(accepted, "Java  language"));
        assertFalse(grader.isTextCorrect(accepted, "J a v a"));
        assertFalse(grader.isTextCorrect(accepted, ""));
        assertFalse(grader.isTextCorrect(accepted, "   "));
        assertFalse(grader.isTextCorrect(accepted, "Python"));
    }

    @Test
    void textMatchingRejectsMissingOrOversizedInputAndMissingKey() {
        assertThrows(IllegalArgumentException.class, () -> grader.isTextCorrect(Set.of("Java"), null));
        assertThrows(IllegalArgumentException.class, () -> grader.isTextCorrect(Set.of("Java"), "a".repeat(256)));
        assertThrows(IllegalStateException.class, () -> grader.isTextCorrect(Set.of(), "Java"));
    }

    private ChoiceOption option(long id, boolean correct) {
        ChoiceOption option = mock(ChoiceOption.class);
        when(option.getId()).thenReturn(id);
        when(option.isCorrect()).thenReturn(correct);
        return option;
    }

    private List<ChoiceOption> options() {
        return List.of(
            option(11L, true),
            option(12L, false),
            option(13L, true)
        );
    }

    @Test
    void exactAnswerIsCorrectRegardlessOfOrder() {
        var options = options();

        assertTrue(grader.isCorrect(
            options, List.of(11L, 13L)));

        assertTrue(grader.isCorrect(
            options, List.of(13L, 11L)));
    }

    @Test
    void missingCorrectOptionIsWrong() {
        assertFalse(grader.isCorrect(
            options(), List.of(11L)));
    }

    @Test
    void selectingExtraOptionIsWrong() {
        assertFalse(grader.isCorrect(
            options(), List.of(11L, 12L, 13L)));
    }

    @Test
    void emptySelectionIsAllowedButWrong() {
        assertFalse(grader.isCorrect(
            options(), List.of()));
    }

    @Test
    void unknownOptionIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> grader.isCorrect(
                options(), List.of(11L, 99L))
        );
    }

    @Test
    void duplicateSelectionIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> grader.isCorrect(
                options(), List.of(11L, 11L))
        );
    }
}
