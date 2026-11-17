package no.hvl.dat250.lms.enrollment.api.dto;

import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;

/**
 * One member of a course. The name and email are included so teachers see who is
 * enrolled without one request per user.
 */
public record EnrollmentResponse(Long userId, CourseRole role, String firstName, String lastName, String email) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        var user = enrollment.getUser();
        return new EnrollmentResponse(user.getUserId(), enrollment.getRole(),
            user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
