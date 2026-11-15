
package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.*;
import no.hvl.dat250.lms.content.service.*;
import no.hvl.dat250.lms.content.service.QuizService.QuestionWithOptions;
import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;
import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator.AnswerInput;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;

import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizService quizService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private QuizAnswerRepository answerRepository;

    @InjectMocks
    private QuizAttemptService attemptService;

    private ChoiceOption option(
            QuizQuestion question,
            long id,
            boolean correct) {

        ChoiceOption option = new ChoiceOption(
            question, "Option " + id, 0, correct
        );

        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private QuizWithQuestions sampleQuiz() {

        Course course = new Course(
            "DAT250", "Test course", Term.AUTUMN, 2026
        );
        ReflectionTestUtils.setField(course, "id", 1L);

        Module module = new Module(course, "Week 1", 0);
        ReflectionTestUtils.setField(module, "id", 2L);

        Quiz quiz = new Quiz(module, "Java Quiz", 0, 2);
        ReflectionTestUtils.setField(quiz, "id", 7L);

        QuizQuestion q1 = new QuizQuestion(
            quiz, "Question 1", 0, null
        );
        QuizQuestion q2 = new QuizQuestion(
            quiz, "Question 2", 1, null
        );

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

    private void stubStudentAndQuiz() {

        when(quizService.findQuizWithQuestions(7L))
            .thenReturn(sampleQuiz());

        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getId()).thenReturn(3L);

        when(enrollmentService.requireActiveStudent(5L, 1L))
            .thenReturn(enrollment);
    }

    @Test
    void validAttemptIsGradedAndSaved() {

        stubStudentAndQuiz();

        when(attemptRepository.save(any(QuizAttempt.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        QuizAttempt attempt = attemptService.submitAttempt(
            7L,
            5L,
            List.of(
                new AnswerInput(11L, List.of(21L, 23L)),
                new AnswerInput(12L, List.of())
            )
        );

        assertEquals(1, attempt.getCorrectCount());
        assertEquals(2, attempt.getTotalQuestions());
        assertNotNull(attempt.getSubmittedAt());

        verify(attemptRepository).save(attempt);

        ArgumentCaptor<QuizAnswer> captor =
            ArgumentCaptor.forClass(QuizAnswer.class);

        verify(answerRepository, times(2))
            .save(captor.capture());

        List<QuizAnswer> savedAnswers =
            captor.getAllValues();

        assertTrue(savedAnswers.get(0).isCorrect());
        assertEquals(
            2,
            savedAnswers.get(0).getSelectedOptions().size()
        );

        assertFalse(savedAnswers.get(1).isCorrect());
        assertTrue(
            savedAnswers.get(1).getSelectedOptions().isEmpty()
        );

        assertSame(
            attempt,
            savedAnswers.get(0).getAttempt()
        );
    }

    @Test
    void exceedingMaxAttemptsIsRejected() {

        stubStudentAndQuiz();

        when(attemptRepository
                .countByEnrollment_IdAndQuiz_Id(3L, 7L))
            .thenReturn(2L);

        assertThrows(
            QuizAttemptLimitExceededException.class,
            () -> attemptService.submitAttempt(
                7L,
                5L,
                List.of(
                    new AnswerInput(11L, List.of(21L, 23L)),
                    new AnswerInput(12L, List.of(31L))
                )
            )
        );

        verify(attemptRepository, never())
            .save(any(QuizAttempt.class));

        verifyNoInteractions(answerRepository);
    }

    @Test
    void invalidOptionIsRejectedWithoutSaving() {

        stubStudentAndQuiz();

        assertThrows(
            IllegalArgumentException.class,
            () -> attemptService.submitAttempt(
                7L,
                5L,
                List.of(
                    new AnswerInput(11L, List.of(999L)),
                    new AnswerInput(12L, List.of(31L))
                )
            )
        );

        verify(attemptRepository, never())
            .save(any(QuizAttempt.class));

        verifyNoInteractions(answerRepository);
    }
    @Test
    void missingEnrollmentIsForbiddenBeforeCountingAttempts() {
        when(quizService.findQuizWithQuestions(7L)).thenReturn(sampleQuiz());
        when(enrollmentService.requireActiveStudent(5L, 1L))
            .thenThrow(new NoSuchElementException("Enrollment not found"));
        assertThrows(AccessDeniedException.class,
            () -> attemptService.submitAttempt(7L, 5L, List.of()));
        verifyNoInteractions(attemptRepository, answerRepository);
    }

    @Test
    void inactiveOrNonStudentEnrollmentIsForbidden() {
        when(quizService.findQuizWithQuestions(7L)).thenReturn(sampleQuiz());
        when(enrollmentService.requireActiveStudent(5L, 1L))
            .thenThrow(new IllegalStateException("Enrollment is inactive"));
        assertThrows(AccessDeniedException.class,
            () -> attemptService.submitAttempt(7L, 5L, List.of()));
        verifyNoInteractions(attemptRepository, answerRepository);
    }

    @Test
    void unrelatedStateFailureIsNotConvertedToAccessDenied() {
        stubStudentAndQuiz();
        IllegalStateException failure = new IllegalStateException("Persistence failure");
        when(attemptRepository.countByEnrollment_IdAndQuiz_Id(3L, 7L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> attemptService.submitAttempt(7L, 5L, List.of())));
    }

}
