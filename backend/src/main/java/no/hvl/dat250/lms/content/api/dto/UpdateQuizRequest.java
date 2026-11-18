package no.hvl.dat250.lms.content.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param maxAttempts attempts allowed per student, at least 1; null keeps the current
 *                    number. Lowering it does not remove attempts already made.
 */
public record UpdateQuizRequest(
    @NotBlank @Size(max = 255)
    String title,

    @Min(1)
    Integer maxAttempts
) {
    public UpdateQuizRequest(String title) {
        this(title, null);
    }
}
