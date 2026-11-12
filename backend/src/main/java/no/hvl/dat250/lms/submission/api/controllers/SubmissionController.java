package no.hvl.dat250.lms.submission.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.identity.CurrentUserService;
import no.hvl.dat250.lms.submission.api.dto.CreateSubmissionRequest;
import no.hvl.dat250.lms.submission.api.dto.SubmissionResponse;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.service.SubmissionService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;

    private final ObjectProvider<CurrentUserService>
        currentUserProvider;

    public SubmissionController(
            SubmissionService submissionService,
            ObjectProvider<CurrentUserService> currentUserProvider) {

        this.submissionService = submissionService;
        this.currentUserProvider = currentUserProvider;
    }

    // Student creates a new individual submission.
    @PostMapping("/contents/{contentId}/submissions")
    public ResponseEntity<SubmissionResponse> submitIndividual(
            @PathVariable Long contentId,
            @Valid @RequestBody CreateSubmissionRequest request) {

        CurrentUserService currentUser =
            currentUserProvider.getIfAvailable();

        if (currentUser == null) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        }

        Long userId = currentUser.requireCurrentUserId();

        Submission saved = submissionService.submitIndividual(
            contentId,
            userId,
            request.type(),
            request.payload()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(SubmissionResponse.from(saved));
    }

    // Student reads one of their own submissions.
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<SubmissionResponse> findOwnSubmission(
            @PathVariable Long submissionId) {

        CurrentUserService currentUser =
            currentUserProvider.getIfAvailable();

        if (currentUser == null) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        }

        Long userId = currentUser.requireCurrentUserId();

        Submission submission =
            submissionService.findOwnSubmission(
                submissionId,
                userId
            );

        return ResponseEntity.ok(
            SubmissionResponse.from(submission)
        );
    }

    // Student reads their own submission history for a Content.
    @GetMapping("/contents/{contentId}/submissions/me")
    public ResponseEntity<List<SubmissionResponse>>
            findMySubmissionHistory(
                @PathVariable Long contentId) {

        CurrentUserService currentUser =
            currentUserProvider.getIfAvailable();

        if (currentUser == null) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        }

        Long userId = currentUser.requireCurrentUserId();

        List<SubmissionResponse> responses =
            submissionService
                .findMySubmissionHistory(contentId, userId)
                .stream()
                .map(SubmissionResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
        // Return 404 for nonexistent or non-owned submissions.
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidArgument() {
        // Reject invalid request parameters.
    }
}
