package no.hvl.dat250.lms.enrollment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;

/** Adds a user to a course; {@code role} defaults to STUDENT. */
public record EnrollRequest(@NotNull @Positive Long userId, CourseRole role) {
}
