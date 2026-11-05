package no.hvl.dat250.lms.course.repository;

import no.hvl.dat250.lms.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
