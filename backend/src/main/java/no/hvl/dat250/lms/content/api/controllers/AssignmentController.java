
package no.hvl.dat250.lms.content.api.controllers;

import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.AssignmentResponse;
import no.hvl.dat250.lms.content.api.dto.CreateAssignmentRequest;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.service.AssignmentService;

import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody CreateAssignmentRequest request) {

        try {
            Assignment assignment = assignmentService.createAssignment(
                courseId,
                moduleId,
                request.title(),
                request.position(),
                request.instructions(),
                request.deadline(),
                request.allowLateSubmission()
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
