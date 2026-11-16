
package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCodingExerciseRequest(

    @NotBlank
    @Size(max = 255)
    String title,

    @NotNull
    @PositiveOrZero
    Integer position,

    @NotBlank
    @Size(max = 10000)
    String instructions,

    @NotBlank
    @Size(max = 2048)
    String starterRepositoryUrl,

    @Size(max = 10000)
    String starterCode,

    Instant deadline,

    boolean allowLateSubmission

) {}
