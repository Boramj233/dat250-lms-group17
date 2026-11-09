
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;

import no.hvl.dat250.lms.content.api.dto.SubmitQuizRequest;
import no.hvl.dat250.lms.content.api.dto.QuizAttemptResponse;

import no.hvl.dat250.lms.content.domain.QuizAttempt;
import no.hvl.dat250.lms.content.service.QuizAttemptService;
import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator.AnswerInput;

import no.hvl.dat250.lms.identity.CurrentUserService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quizzes/{quizId}/attempts")
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    private final ObjectProvider<CurrentUserService>
            currentUserProvider;

    public QuizAttemptController(
            QuizAttemptService attemptService,
            ObjectProvider<CurrentUserService> currentUserProvider) {

        this.attemptService = attemptService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizAttemptResponse submit(
            @PathVariable Long quizId,
            @Valid @RequestBody SubmitQuizRequest request) {

        // Fail closed until Authentication is integrated.
        CurrentUserService currentUser =
                currentUserProvider.getIfAvailable();

        if (currentUser == null) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication integration is not available"
            );
        }

        // Obtain the authenticated LOCAL User ID.
        Long userId = currentUser.requireCurrentUserId();

        // Convert HTTP DTOs into service-level inputs.
        List<AnswerInput> answers = request.answers()
            .stream()
            .map(answer -> new AnswerInput(
                answer.questionId(),
                answer.selectedOptionIds()
            ))
            .toList();

        // Existing business logic handles validation and grading.
        QuizAttempt attempt = attemptService.submitAttempt(
            quizId,
            userId,
            answers
        );

        return QuizAttemptResponse.from(attempt, quizId);
    }
}
