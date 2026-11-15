package no.hvl.dat250.lms.submission.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException;
import no.hvl.dat250.lms.submission.api.dto.CreateSubmissionRequest;
import no.hvl.dat250.lms.submission.api.dto.SubmissionResponse;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.service.SubmissionService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    // Student creates a new individual submission.
    @PostMapping("/contents/{contentId}/submissions")
    public ResponseEntity<SubmissionResponse> submitIndividual(
            @PathVariable Long contentId,
            @Valid @RequestBody CreateSubmissionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        Long userId = currentUser.userId();

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
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        Long userId = currentUser.userId();

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
                @PathVariable Long contentId,
                @AuthenticationPrincipal AuthenticatedUser currentUser) {

        Long userId = currentUser.userId();

        List<SubmissionResponse> responses =
            submissionService
                .findMySubmissionHistory(contentId, userId)
                .stream()
                .map(SubmissionResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @ExceptionHandler(SubmissionDeadlinePassedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleDeadlinePassed() {
        // A valid student cannot submit after a closed deadline.
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleAccessDenied() {
        // Match the authorization exception before its enrollment lookup cause.
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
