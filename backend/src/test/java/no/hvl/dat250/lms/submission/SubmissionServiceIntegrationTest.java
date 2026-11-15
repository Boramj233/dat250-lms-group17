package no.hvl.dat250.lms.submission;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;
import no.hvl.dat250.lms.submission.service.SubmissionService;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:submission_service_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class SubmissionServiceIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionService submissionService;

    private record Fixture(
        User user,
        Course course,
        Assignment assignment,
        Enrollment enrollment
    ) {}

    private User createUser() {

        String suffix = UUID.randomUUID().toString();

        User user = userRepository.save(
            new User(
                "student_" + suffix + "@example.com",
                "Test",
                "Student"
            )
        );

        entityManager.flush();

        return user;
    }

    private Long userId(User user) {

        return (Long) entityManager
            .getEntityManagerFactory()
            .getPersistenceUnitUtil()
            .getIdentifier(user);
    }

    private Fixture createFixture(
            Instant deadline,
            boolean allowLateSubmission) {

        User user = createUser();

        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250 Integration Test",
                "Submission testing",
                Term.AUTUMN,
                2026
            )
        );

        Module module = moduleRepository.saveAndFlush(
            new Module(
                course,
                "Week 5",
                0
            )
        );

        Enrollment enrollment =
            enrollmentRepository.saveAndFlush(
                new Enrollment(
                    user,
                    course,
                    CourseRole.STUDENT
                )
            );

        Assignment assignment = contentRepository.saveAndFlush(
            new Assignment(
                module,
                "JPA Report",
                1,
                "Explain Spring Data JPA.",
                deadline,
                allowLateSubmission
            )
        );

        return new Fixture(
            user,
            course,
            assignment,
            enrollment
        );
    }

    @Test
    void studentSubmitsAndSubmissionIsStoredInDatabase() {

        Fixture f = createFixture(
            Instant.parse("2099-01-01T00:00:00Z"),
            false
        );

        Instant before = Instant.now();

        Submission saved = submissionService.submitIndividual(
            f.assignment().getId(),
            userId(f.user()),
            SubmissionType.TEXT,
            "My integration test report"
        );

        Instant after = Instant.now();

        assertNotNull(saved.getId());
        assertFalse(saved.isLate());

        Long submissionId = saved.getId();
        Long contentId = f.assignment().getId();
        Long enrollmentId = f.enrollment().getId();

        // Force SQL execution, then remove cached entities.
        entityManager.flush();
        entityManager.clear();

        // Reload the Submission from the database.
        Submission stored = submissionRepository
            .findById(submissionId)
            .orElseThrow();

        assertEquals(
            "My integration test report",
            stored.getPayload()
        );

        assertEquals(
            SubmissionType.TEXT,
            stored.getType()
        );

        assertEquals(
            contentId,
            stored.getContent().getId()
        );

        assertEquals(
            enrollmentId,
            stored.getSubmittedBy().getId()
        );

        assertFalse(stored.isLate());

        assertFalse(
            stored.getSubmittedAt().isBefore(before)
        );

        assertFalse(
            stored.getSubmittedAt().isAfter(after)
        );
    }

    @Test
    void allowedLateSubmissionIsStoredWithLateFlag() {

        Fixture f = createFixture(
            Instant.parse("2000-01-01T00:00:00Z"),
            true
        );

        Submission saved = submissionService.submitIndividual(
            f.assignment().getId(),
            userId(f.user()),
            SubmissionType.TEXT,
            "My late assignment"
        );

        Long submissionId = saved.getId();

        entityManager.flush();
        entityManager.clear();

        Submission stored = submissionRepository
            .findById(submissionId)
            .orElseThrow();

        assertTrue(stored.isLate());
        assertEquals(
            "My late assignment",
            stored.getPayload()
        );
    }

    @Test
    void disallowedLateSubmissionIsNotSaved() {

        Fixture f = createFixture(
            Instant.parse("2000-01-01T00:00:00Z"),
            false
        );

        assertThrows(
            SubmissionDeadlinePassedException.class,
            () -> submissionService.submitIndividual(
                f.assignment().getId(),
                userId(f.user()),
                SubmissionType.TEXT,
                "Too late"
            )
        );

        assertEquals(
            0L,
            submissionRepository.count()
        );
    }

    @Test
    void studentFromAnotherCourseCannotSubmit() {

        Fixture f = createFixture(
            Instant.parse("2099-01-01T00:00:00Z"),
            false
        );

        // Create a second student and enroll that student
        // only in a different course.
        User outsider = createUser();

        Course otherCourse = courseRepository.saveAndFlush(
            new Course(
                "Different Course",
                "Other course description",
                Term.AUTUMN,
                2026
            )
        );

        enrollmentRepository.saveAndFlush(
            new Enrollment(
                outsider,
                otherCourse,
                CourseRole.STUDENT
            )
        );

        assertThrows(
            AccessDeniedException.class,
            () -> submissionService.submitIndividual(
                f.assignment().getId(),
                userId(outsider),
                SubmissionType.TEXT,
                "Unauthorized report"
            )
        );

        assertEquals(
            0L,
            submissionRepository.count()
        );
    }

@Test
void submissionHistoryAndDetailAreOwnerScoped() {

    Fixture f = createFixture(
        Instant.parse("2099-01-01T00:00:00Z"),
        false
    );

    // Another student in the SAME course.
    User otherStudent = createUser();

    enrollmentRepository.saveAndFlush(
        new Enrollment(
            otherStudent,
            f.course(),
            CourseRole.STUDENT
        )
    );

    Long ownerId = userId(f.user());
    Long otherUserId = userId(otherStudent);
    Long contentId = f.assignment().getId();

    // Alice submits her assignment.
    Submission saved = submissionService.submitIndividual(
        contentId,
        ownerId,
        SubmissionType.TEXT,
        "Private student answer"
    );

    Long submissionId = saved.getId();

    entityManager.flush();
    entityManager.clear();

    // 1. Verify the Assignment still exists.
    assertTrue(
        contentRepository.findById(contentId).isPresent()
    );

    // 2. Alice can read her own submission.
    Submission ownSubmission =
        submissionService.findOwnSubmission(
            submissionId,
            ownerId
        );

    assertEquals(
        "Private student answer",
        ownSubmission.getPayload()
    );

    // 3. Alice can see her submission history.
    List<Submission> ownerHistory =
        submissionService.findMySubmissionHistory(
            contentId,
            ownerId
        );

    assertEquals(1, ownerHistory.size());

    assertEquals(
        submissionId,
        ownerHistory.get(0).getId()
    );

    // 4. Bob has no submissions for this Assignment.
    List<Submission> otherHistory =
        submissionService.findMySubmissionHistory(
            contentId,
            otherUserId
        );

    assertTrue(otherHistory.isEmpty());

    // 5. Bob cannot read Alice's submission.
    // Keep this expected exception LAST.
    assertThrows(
        NoSuchElementException.class,
        () -> submissionService.findOwnSubmission(
            submissionId,
            otherUserId
        )
    );
}


}
