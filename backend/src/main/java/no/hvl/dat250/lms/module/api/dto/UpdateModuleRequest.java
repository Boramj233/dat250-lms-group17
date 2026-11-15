package no.hvl.dat250.lms.module.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateModuleRequest (

    @NotBlank
    String title,

    @NotNull
    @Min(0)
    Integer position
) {}