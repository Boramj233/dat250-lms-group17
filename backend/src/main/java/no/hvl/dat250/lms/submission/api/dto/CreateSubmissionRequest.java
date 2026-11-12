package no.hvl.dat250.lms.submission.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import no.hvl.dat250.lms.submission.domain.SubmissionType;

public record CreateSubmissionRequest(

    @NotNull(message = "Submission type is required")
    SubmissionType type,

    @NotBlank(message = "Submission payload must not be blank")
    @Size(max = 10000)
    String payload

) {}
