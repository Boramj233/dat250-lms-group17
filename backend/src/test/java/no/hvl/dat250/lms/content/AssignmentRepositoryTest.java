
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AssignmentRepositoryTest {

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
                "Test course",
                Term.AUTUMN,
                2026
            )
        );

        return moduleRepository.saveAndFlush(
            new Module(course, "Week 3", 0)
        );
    }

    @Test
    void assignmentCanBeSavedAndRetrieved() {

        Module module = createModule();

        Instant deadline =
            Instant.parse("2026-11-20T23:00:00Z");

        Assignment assignment = new Assignment(
            module,
            "Week 3 Report",
            2,
            "Write a report about JPA.",
            deadline,
            false
        );

        Assignment saved =
            contentRepository.saveAndFlush(assignment);

        Long assignmentId = saved.getId();

        entityManager.clear();

        Content found = contentRepository
            .findById(assignmentId)
            .orElseThrow();

        assertInstanceOf(Assignment.class, found);

        Assignment stored = (Assignment) found;

        assertEquals(
            "Week 3 Report",
            stored.getTitle()
        );

        assertEquals(
            "Write a report about JPA.",
            stored.getInstructions()
        );

        assertEquals(deadline, stored.getDeadline());

        assertFalse(stored.isAllowLateSubmission());

        assertEquals(
            module.getId(),
            stored.getModule().getId()
        );
    }

    @Test
    void assignmentAppearsInOrderedContentList() {

        Module module = createModule();

        contentRepository.saveAndFlush(
            new StaticPage(
                module,
                "Introduction",
                0,
                "Welcome to Week 3."
            )
        );

        contentRepository.saveAndFlush(
            new Assignment(
                module,
                "JPA Assignment",
                1,
                "Implement JPA entities.",
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
            Assignment.class,
            contents.get(1)
        );

        Assignment assignment =
            (Assignment) contents.get(1);

        assertNull(assignment.getDeadline());

        assertTrue(assignment.isAllowLateSubmission());
    }

    @Test
    void blankInstructionsAreRejected() {

        Module module = createModule();

        assertThrows(
            IllegalArgumentException.class,
            () -> new Assignment(
                module,
                "Invalid Assignment",
                0,
                "   ",
                null,
                false
            )
        );
    }
}
