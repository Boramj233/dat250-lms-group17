package no.hvl.dat250.lms.module;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModuleIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleService moduleService;

    @PersistenceContext
    private EntityManager entityManager;

    // ------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------

    private Course createCourse() {

        Course course = new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
        );

        return courseRepository.saveAndFlush(course);
    }

    // ------------------------------------------------------------
    // Module ordering
    // ------------------------------------------------------------

    @Test
    void moduleOrderSurvivesPersistenceAndReordering() {

        Course course = createCourse();

        Module first = moduleRepository.save(
                new Module(course, "Introduction", 0)
        );

        Module second = moduleRepository.save(
                new Module(course, "Java Basics", 1)
        );

        Module third = moduleRepository.save(
                new Module(course, "Spring Boot", 2)
        );

        // Persist changes and clear the persistence context
        entityManager.flush();
        entityManager.clear();

        // Verify initial order
        List<Module> modules =
                moduleRepository.findByCourse_IdOrderByPositionAsc(
                        course.getId()
                );

        assertEquals(3, modules.size());
        assertEquals("Introduction", modules.get(0).getTitle());
        assertEquals("Java Basics", modules.get(1).getTitle());
        assertEquals("Spring Boot", modules.get(2).getTitle());

        // Change the positions
        moduleService.updateModule(
                course.getId(),
                first.getId(),
                "Introduction",
                2
        );

        moduleService.updateModule(
                course.getId(),
                second.getId(),
                "Java Basics",
                0
        );

        moduleService.updateModule(
                course.getId(),
                third.getId(),
                "Spring Boot",
                1
        );

        // Flush and clear again to force a database reload
        entityManager.flush();
        entityManager.clear();

        // Verify the new order
        List<Module> reordered =
                moduleRepository.findByCourse_IdOrderByPositionAsc(
                        course.getId()
                );

        assertEquals(3, reordered.size());
        assertEquals("Java Basics", reordered.get(0).getTitle());
        assertEquals("Spring Boot", reordered.get(1).getTitle());
        assertEquals("Introduction", reordered.get(2).getTitle());
    }

    // ------------------------------------------------------------
    // Course association
    // ------------------------------------------------------------

    @Test
    void moduleCourseAssociationCanBePersisted() {

        Course course = createCourse();

        Module module = new Module(
                course,
                "Introduction to Java",
                0
        );

        Module saved = moduleRepository.save(module);

        Long moduleId = saved.getId();
        Long courseId = course.getId();

        // Write to database and clear Hibernate's cache
        entityManager.flush();
        entityManager.clear();

        // Reload module from database
        Module reloaded = moduleRepository.findById(moduleId)
                .orElseThrow();

        assertEquals(
                courseId,
                reloaded.getCourse().getId()
        );

        assertEquals(
                "Introduction to Java",
                reloaded.getTitle()
        );
    }

    // ------------------------------------------------------------
    // Database constraints
    // ------------------------------------------------------------

    @Test
    void databaseRejectsModuleWithoutCourse() {

        Module module = new Module(
                null,
                "Invalid Module",
                0
        );

        // Hibernate should reject the missing required course
        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    moduleRepository.saveAndFlush(module);
                }
        );
    }

    @Test
    void databaseRejectsModuleWithUnknownCourse() {

        // This test needs a reference to a course that does not exist.
        Long unknownCourseId = -999999L;

        Course unknownCourse = entityManager.getReference(
                Course.class,
                unknownCourseId
        );

        Module module = new Module(
                unknownCourse,
                "Invalid Module",
                0
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    moduleRepository.saveAndFlush(module);
                }
        );
    }
}