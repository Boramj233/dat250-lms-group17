package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateAssignmentRequest(

    @NotBlank
    @Size(max = 255)
    String title,

    @NotNull
    @PositiveOrZero
    Integer position,

    @NotBlank
    @Size(max = 10000)
    String instructions,

    Instant deadline,

    boolean allowLateSubmission,

    Boolean mandatoryForExam

) {}
