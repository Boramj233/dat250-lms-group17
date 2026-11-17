
package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

import no.hvl.dat250.lms.content.domain.CodingExercise;

public record CodingExerciseResponse(

    Long id,
    Long moduleId,
    String title,
    int position,
    String instructions,
    String starterRepositoryUrl,
    String starterCode,
    Instant deadline,
    boolean allowLateSubmission,
    Instant visibleFrom,
    Instant visibleUntil

) {

    public static CodingExerciseResponse from(
            CodingExercise exercise) {

        return new CodingExerciseResponse(
            exercise.getId(),
            exercise.getModule().getId(),
            exercise.getTitle(),
            exercise.getPosition(),
            exercise.getInstructions(),
            exercise.getStarterRepositoryUrl(),
            exercise.getStarterCode(),
            exercise.getDeadline(),
            exercise.isAllowLateSubmission(),
            exercise.getVisibleFrom(),
            exercise.getVisibleUntil()
        );
    }
}
