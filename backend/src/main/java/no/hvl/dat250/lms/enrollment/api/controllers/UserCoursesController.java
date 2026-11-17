package no.hvl.dat250.lms.enrollment.api.controllers;

import java.util.List;
import no.hvl.dat250.lms.enrollment.api.dto.EnrolledCourseResponse;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The courses a user is enrolled in, for "My courses", the sidebar and the dashboard. */
@RestController
public class UserCoursesController {
    private final EnrollmentService enrollments;

    public UserCoursesController(EnrollmentService enrollments) {
        this.enrollments = enrollments;
    }

    /**
     * The user's courses (active enrollments only) with their role in each, the newest
     * term first. Only the user themselves or an admin; anyone else gets 403.
     */
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    @GetMapping("/api/users/{userId}/courses")
    public List<EnrolledCourseResponse> getCourses(@PathVariable Long userId) {
        return enrollments.activeEnrollmentsWithCourses(userId).stream()
            .map(EnrolledCourseResponse::from)
            .toList();
    }
}
