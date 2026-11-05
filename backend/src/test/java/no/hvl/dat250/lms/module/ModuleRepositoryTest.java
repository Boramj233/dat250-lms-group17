package no.hvl.dat250.lms.module;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DataJpaTest
class ModuleRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void moduleBelongsToCourse() {

        // Arrange: create and save a Course
        Course course = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        Course savedCourse =
                courseRepository.saveAndFlush(course);

        // Create a Module belonging to the Course
        Module module = new Module(
            savedCourse,
            "Java Refresh",
            0
        );

        // Act: save the Module
        Module savedModule =
                moduleRepository.saveAndFlush(module);

        Long moduleId = savedModule.getId();
        Long courseId = savedCourse.getId();

        // Clear managed entities
        entityManager.clear();

        // Read the Module again
        Module found = moduleRepository
                .findById(moduleId)
                .orElseThrow();

        // Assert
        assertEquals("Java Refresh", found.getTitle());
        assertEquals(0, found.getPosition());

        assertEquals(
            courseId,
            found.getCourse().getId()
        );

        assertEquals(
            "DAT250",
            found.getCourse().getTitle()
        );
    }

    @Test
    void modulesAreFilteredByCourseAndOrderedByPosition() {

        // Arrange: create two courses
        Course course1 = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        Course course2 = courseRepository.saveAndFlush(
            new Course(
                "INF264",
                "Introduction to Machine Learning",
                Term.AUTUMN,
                2026
            )
        );

        // Save modules in an intentionally mixed order
        moduleRepository.saveAll(List.of(
            new Module(course1, "JPA", 2),
            new Module(course2, "Machine Learning", 0),
            new Module(course1, "Java Refresh", 0),
            new Module(course1, "Spring Boot", 1)
        ));

        moduleRepository.flush();
        entityManager.clear();

        // Act: query only DAT250 modules
        List<Module> modules =
            moduleRepository.findByCourse_IdOrderByPositionAsc(
                course1.getId()
            );

        // Assert: only three DAT250 modules, correctly ordered
        assertEquals(3, modules.size());

        assertEquals(
            List.of("Java Refresh", "Spring Boot", "JPA"),
            modules.stream()
                .map(Module::getTitle)
                .toList()
        );
    }
}