package no.hvl.dat250.lms.grade.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReleaseGradeRequest(@NotNull @PositiveOrZero Long version) {}
