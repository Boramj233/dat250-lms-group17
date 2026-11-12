
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;

import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CodingExerciseRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ContentRepository contentRepository;

    private Module createModule() {

        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Test Course",
                Term.AUTUMN,
                2026
            )
        );

        return moduleRepository.saveAndFlush(
            new Module(course, "Week 4", 0)
        );
    }

    @Test
    void codingExerciseCanBeSavedAndRetrieved() {

        Module module = createModule();

        Instant deadline =
            Instant.parse("2026-11-25T23:00:00Z");

        CodingExercise exercise = new CodingExercise(
            module,
            "Implement REST API",
            1,
            "Implement GET and POST endpoints.",
            "https://gitlab.com/example/java-rest-starter",
            deadline,
            false
        );

        CodingExercise saved =
            contentRepository.saveAndFlush(exercise);

        Long exerciseId = saved.getId();

        entityManager.clear();

        Content found = contentRepository
            .findById(exerciseId)
            .orElseThrow();

        assertInstanceOf(CodingExercise.class, found);

        CodingExercise stored = (CodingExercise) found;

        assertEquals(
            "Implement REST API",
            stored.getTitle()
        );

        assertEquals(
            "Implement GET and POST endpoints.",
            stored.getInstructions()
        );

        assertEquals(
            "https://gitlab.com/example/java-rest-starter",
            stored.getStarterRepositoryUrl()
        );

        assertEquals(deadline, stored.getDeadline());

        assertFalse(stored.isAllowLateSubmission());
    }

    @Test
    void appearsInMixedContentList() {

        Module module = createModule();

        contentRepository.saveAndFlush(
            new StaticPage(
                module,
                "Introduction",
                0,
                "Welcome to Week 4."
            )
        );

        contentRepository.saveAndFlush(
            new CodingExercise(
                module,
                "REST Exercise",
                1,
                "Build a REST Controller.",
                "https://gitlab.com/example/starter",
                null,
                true
            )
        );

        entityManager.clear();

        List<Content> contents = contentRepository
            .findByModule_IdOrderByPositionAsc(
                module.getId()
            );

        assertEquals(2, contents.size());

        assertInstanceOf(
            StaticPage.class,
            contents.get(0)
        );

        assertInstanceOf(
            CodingExercise.class,
            contents.get(1)
        );

        CodingExercise stored =
            (CodingExercise) contents.get(1);

        assertNull(stored.getDeadline());
        assertTrue(stored.isAllowLateSubmission());
    }

    @Test
    void blankStarterRepositoryIsRejected() {

        Module module = createModule();

        assertThrows(
            IllegalArgumentException.class,
            () -> new CodingExercise(
                module,
                "Invalid Exercise",
                0,
                "Some instructions",
                "   ",
                null,
                false
            )
        );
    }
}
