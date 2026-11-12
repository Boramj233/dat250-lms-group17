
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.List;

import no.hvl.dat250.lms.content.api.controllers.QuizAttemptController;
import no.hvl.dat250.lms.content.api.dto.SubmitQuizRequest;
import no.hvl.dat250.lms.content.domain.QuizAttempt;
import no.hvl.dat250.lms.content.service.QuizAttemptService;
import no.hvl.dat250.lms.identity.CurrentUserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptControllerTest {

    @Mock
    private QuizAttemptService attemptService;

    @Mock
    private ObjectProvider<CurrentUserService> currentUserProvider;

    @Test
    void rejectsSubmissionWhenAuthenticationIsUnavailable() {

        QuizAttemptController controller =
            new QuizAttemptController(
                attemptService,
                currentUserProvider
            );

        SubmitQuizRequest request = new SubmitQuizRequest(
            List.of(
                new SubmitQuizRequest.Answer(
                    11L,
                    List.of(21L)
                )
            )
        );

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> controller.submit(7L, request)
            );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );

        verifyNoInteractions(attemptService);
    }

    @Test
    void usesAuthenticatedUserIdForSubmission() {

        CurrentUserService currentUser =
            mock(CurrentUserService.class);

        when(currentUserProvider.getIfAvailable())
            .thenReturn(currentUser);

        when(currentUser.requireCurrentUserId())
            .thenReturn(5L);

        QuizAttempt savedAttempt =
            mock(QuizAttempt.class);

        when(savedAttempt.getId()).thenReturn(100L);

        when(savedAttempt.getSubmittedAt()).thenReturn(
            Instant.parse("2026-11-09T12:00:00Z")
        );

        when(savedAttempt.getCorrectCount()).thenReturn(1);
        when(savedAttempt.getTotalQuestions()).thenReturn(2);

        when(attemptService.submitAttempt(
                eq(7L),
                eq(5L),
                anyList()
        )).thenReturn(savedAttempt);

        QuizAttemptController controller =
            new QuizAttemptController(
                attemptService,
                currentUserProvider
            );

        SubmitQuizRequest request = new SubmitQuizRequest(
            List.of(
                new SubmitQuizRequest.Answer(
                    11L,
                    List.of(21L, 23L)
                )
            )
        );

        var response = controller.submit(7L, request);

        assertEquals(100L, response.id());
        assertEquals(7L, response.quizId());
        assertEquals(1, response.correctCount());
        assertEquals(2, response.totalQuestions());

        verify(attemptService).submitAttempt(
            eq(7L),
            eq(5L),
            argThat(answers ->
                answers.size() == 1 &&
                answers.get(0).questionId().equals(11L) &&
                answers.get(0).selectedOptionIds()
                    .equals(List.of(21L, 23L))
            )
        );
    }
}
