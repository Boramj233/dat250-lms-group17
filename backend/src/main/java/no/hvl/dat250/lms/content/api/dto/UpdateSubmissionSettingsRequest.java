package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;

public record UpdateSubmissionSettingsRequest(
    Instant deadline,
    @NotNull Boolean allowLateSubmission
) {
}
