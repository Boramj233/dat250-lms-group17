
package no.hvl.dat250.lms.content.api.controllers;

import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateCodingExerciseRequest;
import no.hvl.dat250.lms.content.api.dto.UpdateCodingExerciseRequest;
import no.hvl.dat250.lms.content.api.dto.CodingExerciseResponse;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.service.CodingExerciseService;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CodingExerciseController {

    private final CodingExerciseService codingExerciseService;

    public CodingExerciseController(
            CodingExerciseService codingExerciseService) {

        this.codingExerciseService = codingExerciseService;
    }

    @PostMapping(
        "/api/courses/{courseId}/modules/{moduleId}/contents/coding-exercises"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public CodingExerciseResponse create(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateCodingExerciseRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        try {
            CodingExercise exercise =
                codingExerciseService.createCodingExercise(
                    courseId,
                    moduleId,
                    request.title(),
                    request.position(),
                    request.instructions(),
                    request.starterRepositoryUrl(),
                    request.starterCode(),
                    request.deadline(),
                    request.allowLateSubmission(),
                    currentUser.userId()
                );

            return CodingExerciseResponse.from(exercise);

        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex
            );
        }
    }

    @PreAuthorize("@courseAccess.isTeacher(authentication, @codingExerciseService.courseId(#exerciseId))")
    @PutMapping("/api/coding-exercises/{exerciseId}")
    public ResponseEntity<CodingExerciseResponse> updateCodingExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody UpdateCodingExerciseRequest request) {
        return ResponseEntity.ok(CodingExerciseResponse.from(codingExerciseService.updateDetails(
            exerciseId, request.title(), request.instructions(), request.starterRepositoryUrl(), request.starterCode())));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleForbidden() {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleBadRequest() {
    }

    @PreAuthorize("@courseAccess.isEnrolled(authentication, @codingExerciseService.courseId(#exerciseId))")
    @GetMapping("/api/coding-exercises/{exerciseId}")
    public CodingExerciseResponse get(
            @PathVariable Long exerciseId) {

        try {
            return CodingExerciseResponse.from(
                codingExerciseService.findCodingExercise(
                    exerciseId
                )
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
