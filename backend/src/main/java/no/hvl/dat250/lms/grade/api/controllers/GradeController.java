package no.hvl.dat250.lms.grade.api.controllers;

import java.util.NoSuchElementException;
import jakarta.validation.Valid;
import no.hvl.dat250.lms.grade.api.dto.*;
import no.hvl.dat250.lms.grade.service.GradeConflictException;
import no.hvl.dat250.lms.grade.service.GradingService;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GradeController {
    private final GradingService gradingService;

    public GradeController(GradingService gradingService) { this.gradingService = gradingService; }

    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<GradeReviewResponse> save(
            @PathVariable Long submissionId,
            @Valid @RequestBody SaveGradeRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(gradingService.save(submissionId, currentUser.userId(),
            request.score(), request.feedback(), request.version(), request.passed(), request.visibleFrom()));
    }

    @PostMapping("/submissions/{submissionId}/grade/release")
    public ResponseEntity<GradeReviewResponse> release(
            @PathVariable Long submissionId,
            @Valid @RequestBody ReleaseGradeRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(gradingService.release(submissionId, currentUser.userId(), request.version()));
    }

    @GetMapping("/submissions/{submissionId}/grade/review")
    public ResponseEntity<GradeReviewResponse> review(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(gradingService.review(submissionId, currentUser.userId()));
    }

    @GetMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<ReleasedGradeResponse> ownReleased(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(gradingService.ownReleased(submissionId, currentUser.userId()));
    }

    /**
     * Releases every saved grade of the content's latest submissions that has a Pass/Fail
     * decision; the others are counted as undecided. Active teachers of the course only.
     */
    @PostMapping("/contents/{contentId}/grades/release")
    public ResponseEntity<ReleaseAllGradesResponse> releaseAll(
            @PathVariable Long contentId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return response(gradingService.releaseAll(contentId, currentUser.userId()));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> forbidden() {
        return failure(HttpStatus.FORBIDDEN, "Active course teacher required");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound() {
        return failure(HttpStatus.NOT_FOUND, "Grade or submission not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid() {
        return failure(HttpStatus.BAD_REQUEST, "Invalid grade request");
    }

    @ExceptionHandler(GradeConflictException.class)
    public ResponseEntity<ProblemDetail> conflict(GradeConflictException exception) {
        return failure(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class,
        PessimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> concurrentConflict() {
        return failure(HttpStatus.CONFLICT, "Concurrent grade change; refresh and retry");
    }

    private ResponseEntity<ProblemDetail> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore().cachePrivate())
            .body(ProblemDetail.forStatusAndDetail(status, message));
    }
}
