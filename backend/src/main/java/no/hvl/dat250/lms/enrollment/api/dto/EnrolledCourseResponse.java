package no.hvl.dat250.lms.enrollment.api.dto;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;

/** A course the user is enrolled in: the course's fields plus the user's role in it. */
public record EnrolledCourseResponse(Long id, String title, String description, Term term, int year,
                                     CourseRole role) {

    public static EnrolledCourseResponse from(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        return new EnrolledCourseResponse(course.getId(), course.getTitle(), course.getDescription(),
            course.getTerm(), course.getYear(), enrollment.getRole());
    }
}
