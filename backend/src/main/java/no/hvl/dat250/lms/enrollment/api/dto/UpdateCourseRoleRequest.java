package no.hvl.dat250.lms.enrollment.api.dto;

import jakarta.validation.constraints.NotNull;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;

/** Makes a member a TEACHER or a STUDENT of the course. */
public record UpdateCourseRoleRequest(@NotNull CourseRole role) {
}
