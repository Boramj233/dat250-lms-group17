package no.hvl.dat250.lms.interaction;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.interaction.domain.StudentInteraction;
import no.hvl.dat250.lms.interaction.repository.StudentInteractionRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StudentInteractionRepositoryTest {

    @Autowired
    private StudentInteractionRepository interactionRepository;

    @Autowired
    private EntityManager entityManager;

    private Module module;
    private User student;

    @BeforeEach
    void setUp() {

        Course course = new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
        );

        entityManager.persist(course);

        module = new Module(course, "Java Refresh", 0);
        entityManager.persist(module);

        student = persistStudent("student@example.com");

        entityManager.flush();
    }

    private User persistStudent(String email) {

        User user = new User(
                email,
                "Test",
                "Student"
        );

        entityManager.persist(user);

        return user;
    }

    private StaticPage createContent(
            String title,
            int position,
            boolean required) {

        StaticPage page = new StaticPage(
                module,
                title,
                position,
                "Test content"
        );

        page.setRequiredForModuleCompletion(required);

        entityManager.persist(page);

        return page;
    }

    private void completeContent(User user, StaticPage content) {

        StudentInteraction interaction =
                new StudentInteraction(user, content);

        interaction.markCompleted();

        entityManager.persist(interaction);
    }

    private long incompleteCount() {

        entityManager.flush();
        entityManager.clear();

        return interactionRepository.countIncompleteRequiredContent(
                module.getId(),
                student.getUserId()
        );
    }

    @Test
    void emptyModuleHasNoIncompleteRequiredContent() {

        assertEquals(0L, incompleteCount());
    }

    @Test
    void uncompletedRequiredContentIsCounted() {

        createContent("Introduction", 0, true);

        assertEquals(1L, incompleteCount());
    }

    @Test
    void completedRequiredContentIsNotCounted() {

        StaticPage content =
                createContent("Introduction", 0, true);

        completeContent(student, content);

        assertEquals(0L, incompleteCount());
    }

    @Test
    void onlyIncompleteRequiredContentIsCounted() {

        StaticPage first =
                createContent("Introduction", 0, true);

        createContent("Exercises", 1, true);

        completeContent(student, first);

        assertEquals(1L, incompleteCount());
    }

    @Test
    void optionalContentDoesNotPreventCompletion() {

        createContent("Optional Reading", 0, false);

        assertEquals(0L, incompleteCount());
    }

    @Test
    void completedContentFromAnotherStudentDoesNotCount() {

        StaticPage content =
                createContent("Introduction", 0, true);

        User otherStudent =
                persistStudent("other@example.com");

        completeContent(otherStudent, content);

        assertEquals(1L, incompleteCount());
    }

    @Test
    void interactionWithoutCompletionTimestampDoesNotCount() {

        StaticPage content =
                createContent("Introduction", 0, true);

        StudentInteraction interaction =
                new StudentInteraction(student, content);

        // Do not call markCompleted().
        entityManager.persist(interaction);

        assertEquals(1L, incompleteCount());
    }

    @Test
    void findInteractionByStudentAndContent() {

        StaticPage content =
                createContent("Introduction", 0, true);

        completeContent(student, content);

        entityManager.flush();
        entityManager.clear();

        StudentInteraction found = interactionRepository
                .findByStudent_UserIdAndContent_Id(
                        student.getUserId(),
                        content.getId()
                )
                .orElseThrow();

        assertTrue(found.isCompleted());
        assertNotNull(found.getCompletedAt());
    }

    @Test
    void interactionCannotBeFoundForAnotherStudent() {

        StaticPage content =
                createContent("Introduction", 0, true);

        User otherStudent =
                persistStudent("other@example.com");

        completeContent(otherStudent, content);

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                interactionRepository
                        .findByStudent_UserIdAndContent_Id(
                                student.getUserId(),
                                content.getId()
                        )
                        .isEmpty()
        );
    }
}