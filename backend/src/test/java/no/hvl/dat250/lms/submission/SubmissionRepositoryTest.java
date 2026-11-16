package no.hvl.dat250.lms.submission;

import java.time.Instant;
import java.util.List;

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
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SubmissionRepositoryTest {

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

    private record Fixture(
        Module module,
        Assignment assignment,
        Enrollment enrollment
    ) {}

    private Fixture createFixture() {

        User user = userRepository.save(
            new User(
                "submission_alice@example.com",
                "Alice",
                "Submission"
            )
        );

        entityManager.flush();

        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        Module module = moduleRepository.saveAndFlush(
            new Module(course, "Week 4", 0)
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
                "Spring Data JPA Report",
                1,
                "Write a report about JPA.",
                null,
                true
            )
        );

        return new Fixture(module, assignment, enrollment);
    }

    @Test
    void savesAndRetrievesIndividualSubmission() {

        Fixture f = createFixture();

        Instant time =
            Instant.parse("2026-11-10T12:00:00Z");

        Submission submission = new Submission(
            f.assignment(),
            f.enrollment(),
            SubmissionType.TEXT,
            "My completed JPA report.",
            time,
            false
        );

        Submission saved =
            submissionRepository.saveAndFlush(submission);

        Long submissionId = saved.getId();

        assertNotNull(submissionId);

        entityManager.clear();

        Submission stored = submissionRepository
            .findById(submissionId)
            .orElseThrow();

        assertEquals(
            f.assignment().getId(),
            stored.getContent().getId()
        );

        assertEquals(
            f.enrollment().getId(),
            stored.getSubmittedBy().getId()
        );

        assertEquals(
            SubmissionType.TEXT,
            stored.getType()
        );

        assertEquals(
            "My completed JPA report.",
            stored.getPayload()
        );

        assertEquals(time, stored.getSubmittedAt());
        assertFalse(stored.isLate());
    }

    @Test
    void keepsMultipleSubmissionsInHistory() {

        Fixture f = createFixture();

        submissionRepository.saveAndFlush(
            new Submission(
                f.assignment(),
                f.enrollment(),
                SubmissionType.TEXT,
                "First version",
                Instant.parse("2026-11-10T10:00:00Z"),
                false
            )
        );

        submissionRepository.saveAndFlush(
            new Submission(
                f.assignment(),
                f.enrollment(),
                SubmissionType.TEXT,
                "Updated version",
                Instant.parse("2026-11-10T12:00:00Z"),
                false
            )
        );

        entityManager.clear();

        List<Submission> history = submissionRepository
            .findByContent_IdAndSubmittedBy_IdOrderBySubmittedAtDescIdDesc(
                f.assignment().getId(),
                f.enrollment().getId()
            );

        assertEquals(2, history.size());

        // Newest submission comes first.
        assertEquals(
            "Updated version",
            history.get(0).getPayload()
        );

        assertEquals(
            "First version",
            history.get(1).getPayload()
        );

        assertEquals(
            2L,
            submissionRepository.countByContent_IdAndSubmittedBy_Id(
                f.assignment().getId(),
                f.enrollment().getId()
            )
        );
    }

    @Test
    void supportsCodingExerciseSubmission() {

        Fixture f = createFixture();

        CodingExercise exercise = contentRepository.saveAndFlush(
            new CodingExercise(
                f.module(),
                "REST API Exercise",
                2,
                "Implement a REST API.",
                "https://gitlab.com/example/starter",
                null,
                true
            )
        );

        Submission submission = submissionRepository.saveAndFlush(
            new Submission(
                exercise,
                f.enrollment(),
                SubmissionType.GIT_URL,
                "https://gitlab.com/example/alice-solution",
                Instant.parse("2026-11-10T12:00:00Z"),
                false
            )
        );

        Long submissionId = submission.getId();

        entityManager.clear();

        Submission stored = submissionRepository
            .findById(submissionId)
            .orElseThrow();

        assertEquals(
            SubmissionType.GIT_URL,
            stored.getType()
        );

        assertEquals(
            exercise.getId(),
            stored.getContent().getId()
        );
    }

    @Test
    void rejectsStaticPageSubmission() {

        Fixture f = createFixture();

        StaticPage page = new StaticPage(
            f.module(),
            "Reading Notes",
            3,
            "Read this before the assignment."
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Submission(
                page,
                f.enrollment(),
                SubmissionType.TEXT,
                "Some answer",
                Instant.parse("2026-11-10T12:00:00Z"),
                false
            )
        );
    }

    @Test
    void rejectsBlankPayload() {

        Fixture f = createFixture();

        assertThrows(
            IllegalArgumentException.class,
            () -> new Submission(
                f.assignment(),
                f.enrollment(),
                SubmissionType.TEXT,
                "   ",
                Instant.parse("2026-11-10T12:00:00Z"),
                false
            )
        );
    }
    @Test
    void fileMetadataRoundTripsAlongsideLegacyHistory() {
        Fixture f = createFixture();
        Instant time = Instant.parse("2026-11-16T12:00:00Z");
        Submission old = submissionRepository.saveAndFlush(new Submission(
                f.assignment(), f.enrollment(), SubmissionType.TEXT, "Earlier answer", time, false));
        var metadata = new no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata(
                "lms-submissions", "submissions/version-2", "report.pdf", "application/pdf", 123, "a".repeat(64));
        Submission saved = submissionRepository.saveAndFlush(Submission.file(
                f.assignment(), f.enrollment(), metadata, time, false));
        entityManager.clear();
        Submission stored = submissionRepository.findById(saved.getId()).orElseThrow();
        assertEquals(SubmissionType.FILE, stored.getType());
        assertNull(stored.getPayload());
        assertEquals("lms-submissions", stored.getFile().getBucket());
        assertEquals("submissions/version-2", stored.getFile().getObjectKey());
        assertEquals("report.pdf", stored.getFile().getOriginalFilename());
        assertEquals("application/pdf", stored.getFile().getMediaType());
        assertEquals(123, stored.getFile().getSizeBytes());
        assertEquals("a".repeat(64), stored.getFile().getSha256());
        Submission legacy = submissionRepository.findById(old.getId()).orElseThrow();
        assertEquals("Earlier answer", legacy.getPayload());
        assertNull(legacy.getFile());
        var history = submissionRepository.findByContent_IdAndSubmittedBy_IdOrderBySubmittedAtDescIdDesc(
                f.assignment().getId(), f.enrollment().getId());
        assertEquals(java.util.List.of(saved.getId(), old.getId()), history.stream().map(Submission::getId).toList());
    }

}
