package no.hvl.dat250.lms.content.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStaticPageRequest(
    @NotBlank @Size(max = 255)
    String title,
    @NotBlank @Size(max = 10000)
    String body
) {
}
