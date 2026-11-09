
package no.hvl.dat250.lms.content;

import java.util.List;

import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator;
import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator.AnswerInput;
import no.hvl.dat250.lms.content.service.QuizService.QuestionWithOptions;
import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;
import no.hvl.dat250.lms.module.domain.Module;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class QuizSubmissionEvaluatorTest {

    private final QuizSubmissionEvaluator evaluator =
        new QuizSubmissionEvaluator();

    private ChoiceOption option(
            QuizQuestion question,
            long id,
            boolean correct) {

        ChoiceOption option = new ChoiceOption(
            question, "Option " + id, 0, correct);

        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private QuizWithQuestions sampleQuiz() {

        Quiz quiz = new Quiz(
            mock(Module.class), "Java Quiz", 0, 2
        );

        QuizQuestion q1 = new QuizQuestion(
            quiz, "Question 1", 0, null);

        QuizQuestion q2 = new QuizQuestion(
            quiz, "Question 2", 1, null);

        ReflectionTestUtils.setField(q1, "id", 11L);
        ReflectionTestUtils.setField(q2, "id", 12L);

        return new QuizWithQuestions(
            quiz,
            List.of(
                new QuestionWithOptions(
                    q1,
                    List.of(
                        option(q1, 21L, true),
                        option(q1, 22L, false),
                        option(q1, 23L, true)
                    )
                ),
                new QuestionWithOptions(
                    q2,
                    List.of(
                        option(q2, 31L, true),
                        option(q2, 32L, false)
                    )
                )
            )
        );
    }

    @Test
    void evaluatesCompleteQuiz() {

        var result = evaluator.evaluate(
            sampleQuiz(),
            List.of(
                new AnswerInput(11L, List.of(23L, 21L)),
                new AnswerInput(12L, List.of())
            )
        );

        assertEquals(1, result.correctCount());
        assertEquals(2, result.totalQuestions());

        assertTrue(result.answers().get(0).correct());
        assertFalse(result.answers().get(1).correct());
    }

    @Test
    void rejectsMissingQuestion() {

        assertThrows(
            IllegalArgumentException.class,
            () -> evaluator.evaluate(
                sampleQuiz(),
                List.of(
                    new AnswerInput(11L, List.of(21L, 23L))
                )
            )
        );
    }

    @Test
    void rejectsDuplicateQuestion() {

        assertThrows(
            IllegalArgumentException.class,
            () -> evaluator.evaluate(
                sampleQuiz(),
                List.of(
                    new AnswerInput(11L, List.of(21L)),
                    new AnswerInput(11L, List.of(23L))
                )
            )
        );
    }

    @Test
    void rejectsUnknownQuestion() {

        assertThrows(
            IllegalArgumentException.class,
            () -> evaluator.evaluate(
                sampleQuiz(),
                List.of(
                    new AnswerInput(11L, List.of(21L)),
                    new AnswerInput(999L, List.of())
                )
            )
        );
    }

    @Test
    void rejectsOptionFromAnotherQuestion() {

        assertThrows(
            IllegalArgumentException.class,
            () -> evaluator.evaluate(
                sampleQuiz(),
                List.of(
                    new AnswerInput(11L, List.of(31L)),
                    new AnswerInput(12L, List.of())
                )
            )
        );
    }
}
