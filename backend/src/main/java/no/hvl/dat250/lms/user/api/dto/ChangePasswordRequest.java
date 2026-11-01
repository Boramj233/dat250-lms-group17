package no.hvl.dat250.lms.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Plaintext credentials are only used for verification/encoding and never persisted. */
public record ChangePasswordRequest(@NotBlank String currentPassword,
                                    @NotBlank @Size(min = 8, max = 72) String newPassword) {
    @Override
    public String toString() { return "ChangePasswordRequest[credentials=REDACTED]"; }
}
