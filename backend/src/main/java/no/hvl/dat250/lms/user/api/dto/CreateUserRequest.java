package no.hvl.dat250.lms.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Local profile data for an externally managed account; no credentials are accepted. */
public record CreateUserRequest(@NotBlank @Size(max = 255) String firstName,
                                 @NotBlank @Size(max = 255) String lastName,
                                 @NotBlank @Email @Size(max = 255) String email) {}
