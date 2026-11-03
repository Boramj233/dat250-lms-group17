package no.hvl.dat250.lms.course.api.dto;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;

public record CourseResponse(
    Long id,
    String title,
    String description,
    Term term,
    int year
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getTerm(),
            course.getYear()
        );
    }
}
