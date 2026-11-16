package no.hvl.dat250.lms.submission.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.submission.api.dto.SubmissionReviewResponse;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SubmissionReviewController {
    private final SubmissionService submissionService;

    public SubmissionReviewController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/contents/{contentId}/submissions/review")
    public ResponseEntity<Page<SubmissionReviewResponse>> latest(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(submissionService.reviewLatest(contentId, currentUser.userId(), page, size));
    }

    @GetMapping("/submissions/{submissionId}/review")
    public ResponseEntity<SubmissionReviewResponse> detail(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(submissionService.reviewSubmission(submissionId, currentUser.userId()));
    }

    @GetMapping("/submissions/{submissionId}/review/history")
    public ResponseEntity<List<SubmissionReviewResponse>> history(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(submissionService.reviewHistory(submissionId, currentUser.userId()));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> invalid() {
        return ResponseEntity.badRequest().build();
    }
}
