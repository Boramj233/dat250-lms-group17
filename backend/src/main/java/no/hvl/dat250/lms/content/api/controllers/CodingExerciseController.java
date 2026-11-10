
package no.hvl.dat250.lms.content.api.controllers;

import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateCodingExerciseRequest;
import no.hvl.dat250.lms.content.api.dto.CodingExerciseResponse;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.service.CodingExerciseService;

import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody CreateCodingExerciseRequest request) {

        try {
            CodingExercise exercise =
                codingExerciseService.createCodingExercise(
                    courseId,
                    moduleId,
                    request.title(),
                    request.position(),
                    request.instructions(),
                    request.starterRepositoryUrl(),
                    request.deadline(),
                    request.allowLateSubmission()
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
