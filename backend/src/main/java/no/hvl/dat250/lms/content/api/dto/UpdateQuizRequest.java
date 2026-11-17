package no.hvl.dat250.lms.content.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateQuizRequest(
    @NotBlank @Size(max = 255)
    String title
) {
}
