package no.hvl.dat250.lms.group.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateGroupRequest(
        @NotNull @Positive Integer groupNumber,
        @Positive Integer maxGroupSize
) {}
