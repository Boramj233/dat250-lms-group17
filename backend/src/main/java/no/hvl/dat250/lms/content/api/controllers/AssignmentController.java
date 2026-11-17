
package no.hvl.dat250.lms.content.api.controllers;

import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.AssignmentResponse;
import no.hvl.dat250.lms.content.api.dto.CreateAssignmentRequest;
import no.hvl.dat250.lms.content.api.dto.UpdateAssignmentExamRequirementRequest;
import no.hvl.dat250.lms.content.api.dto.UpdateAssignmentRequest;
import no.hvl.dat250.lms.content.api.dto.UpdateSubmissionSettingsRequest;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.service.AssignmentService;
import no.hvl.dat250.lms.security.AuthenticatedUser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(
            AssignmentService assignmentService) {

        this.assignmentService = assignmentService;
    }

    @PostMapping(
        "/api/courses/{courseId}/modules/{moduleId}/contents/assignments"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse createAssignment(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        try {
            Assignment assignment = assignmentService.createAssignment(
                courseId,
                moduleId,
                request.title(),
                request.position(),
                request.instructions(),
                request.deadline(),
                request.allowLateSubmission(),
                Boolean.TRUE.equals(request.mandatoryForExam()),
                currentUser.userId()
            );

            return AssignmentResponse.from(assignment);

        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex
            );
        }
    }

    @PreAuthorize("@courseAccess.isTeacher(authentication, @assignmentService.courseId(#assignmentId))")
    @PutMapping("/api/assignments/{assignmentId}/submission-settings")
    public ResponseEntity<AssignmentResponse> updateSubmissionSettings(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateSubmissionSettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(AssignmentResponse.from(assignmentService.updateSubmissionSettings(
            assignmentId, request.deadline(), request.allowLateSubmission(), currentUser.userId())));
    }

    @PreAuthorize("@courseAccess.isTeacher(authentication, @assignmentService.courseId(#assignmentId))")
    @PutMapping("/api/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        return ResponseEntity.ok(AssignmentResponse.from(
            assignmentService.updateDetails(assignmentId, request.title(), request.instructions())));
    }

    @PatchMapping("/api/assignments/{assignmentId}/exam-requirement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateExamRequirement(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateAssignmentExamRequirementRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        assignmentService.updateExamRequirement(
            assignmentId,
            currentUser.userId(),
            request.mandatoryForExam()
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleAccessDenied() {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidArgument() {
    }

    @PreAuthorize("""
        @courseAccess.isEnrolled(authentication, @assignmentService.courseId(#assignmentId))
        and (@courseAccess.isTeacher(authentication, @assignmentService.courseId(#assignmentId))
            or @contentService.isVisible(#assignmentId))
        """)
    @GetMapping("/api/assignments/{assignmentId}")
    public AssignmentResponse getAssignment(
            @PathVariable Long assignmentId) {

        try {
            return AssignmentResponse.from(
                assignmentService.findAssignment(assignmentId)
            );

        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex
            );
        }
    }
}
