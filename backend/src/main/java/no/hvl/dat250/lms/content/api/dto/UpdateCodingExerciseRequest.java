package no.hvl.dat250.lms.content.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCodingExerciseRequest(
    @NotBlank @Size(max = 255)
    String title,
    @NotBlank @Size(max = 10000)
    String instructions,
    @NotBlank @Size(max = 2048)
    String starterRepositoryUrl,
    @Size(max = 10000)
    String starterCode
) {
}
