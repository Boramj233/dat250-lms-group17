package no.hvl.dat250.lms.course.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

@Service
public class CourseService {

    private final CourseRepository repository;
    public CourseService(CourseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Course createCourse(
        String title,
        String description,
        Term term,
        int year
    ) {
        if (title == null || title.isBlank()){
            throw new IllegalArgumentException(
                "Course title must not be blank"
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                "Course description must not be blank"
            );
        }

        if (term == null) {
            throw new IllegalArgumentException(
                "Course term must not be null");
        }

        if (year <= 0) {
            throw new IllegalArgumentException(
                "Course year must be positive");
        }

        Course course = new Course(title, description, term, year);

        return repository.save(course);
    }

    @Transactional
    public Optional<Course> findCourseById(Long id) {
        return repository.findById(id);
    }
}
