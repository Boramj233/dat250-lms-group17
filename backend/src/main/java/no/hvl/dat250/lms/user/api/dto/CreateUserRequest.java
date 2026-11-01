package no.hvl.dat250.lms.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Administrator-supplied profile and temporary plaintext password; identity/access are generated. */
public record CreateUserRequest(@NotBlank @Size(max = 255) String firstName,
                                 @NotBlank @Size(max = 255) String lastName,
                                 @NotBlank @Size(min = 8, max = 72) String temporaryPassword) {
    @Override
    public String toString() { return "CreateUserRequest[credentials=REDACTED]"; }
}
