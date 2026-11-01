package no.hvl.dat250.lms.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable profile data. Email, access, and passwords cannot be changed here. */
public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName
) {}
