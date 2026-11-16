
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.api.dto.SubmitQuizRequest;
import no.hvl.dat250.lms.content.api.dto.QuizAttemptResponse;

import no.hvl.dat250.lms.content.service.QuizAttemptService.AttemptResult;
import no.hvl.dat250.lms.content.service.QuizAttemptService;
import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator.AnswerInput;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.content.service.QuizAttemptLimitExceededException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quizzes/{quizId}/attempts")
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    public QuizAttemptController(QuizAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizAttemptResponse submit(
            @PathVariable Long quizId,
            @Valid @RequestBody SubmitQuizRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        Long userId = currentUser.userId();

        // Convert HTTP DTOs into service-level inputs.
        List<AnswerInput> answers = request.answers()
            .stream()
            .map(answer -> new AnswerInput(
                answer.questionId(),
                answer.selectedOptionIds(),
                answer.textAnswer(),
                answer.orderedItemIds()
            ))
            .toList();

        // Existing business logic handles validation and grading.
        AttemptResult result = attemptService.submitAttemptWithFeedback(
            quizId,
            userId,
            answers
        );

        return QuizAttemptResponse.from(result, quizId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleAccessDenied() {
        // Match the authorization exception before its enrollment lookup cause.
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidArgument() {}

    @ExceptionHandler(QuizAttemptLimitExceededException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleAttemptLimitExceeded() {}
}
