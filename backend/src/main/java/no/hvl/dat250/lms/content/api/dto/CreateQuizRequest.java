package no.hvl.dat250.lms.content.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuizRequest(
    @NotBlank
    String title,

    @NotNull
    @Min(0)
    Integer position,

    @NotNull
    @Min(1)
    Integer maxAttempts
) {}