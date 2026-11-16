package no.hvl.dat250.lms.grade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import no.hvl.dat250.lms.grade.api.dto.GradeReviewResponse;
import no.hvl.dat250.lms.grade.repository.GradeRepository;
import no.hvl.dat250.lms.grade.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters, JPA commits and separate transactions; not live Keycloak E2E. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:grade_submission_concurrency_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradeSubmissionConcurrencyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager entities;
    @Autowired PlatformTransactionManager transactions;
    @Autowired GradingService grading;
    @Autowired SubmissionService submissions;
    @Autowired GradeRepository grades;

    private <T> T save(T entity) { entities.persist(entity); entities.flush(); return entity; }
    private User user() {
        return save(new User("grade_" + UUID.randomUUID() + "@example.com", "Test", "User"));
    }
    private record Fixture(User owner, User teacher, User outsider, User inactive,
                           User otherStudent, Long contentId, Long submissionId, Long enrollmentId) {}
    private Fixture fixture() {
        return new TransactionTemplate(transactions).execute(status -> {
            Course course = save(new Course("DAT250", "Grading", Term.AUTUMN, 2026));
            Course otherCourse = save(new Course("Other", "Other", Term.AUTUMN, 2026));
            User owner = user(), teacher = user(), outsider = user(), inactive = user(), other = user();
            Enrollment student = save(new Enrollment(owner, course, CourseRole.STUDENT));
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            save(new Enrollment(outsider, otherCourse, CourseRole.TEACHER));
            Enrollment inactiveEnrollment = save(new Enrollment(inactive, course, CourseRole.TEACHER));
            inactiveEnrollment.deactivate();
            save(new Enrollment(other, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            Assignment assignment = save(new Assignment(module, "Report", 0, "Write", null, false));
            Submission submission = save(new Submission(assignment, student, SubmissionType.TEXT,
                "Answer", Instant.parse("2026-01-01T12:00:00Z"), false));
            return new Fixture(owner, teacher, outsider, inactive, other,
                assignment.getId(), submission.getId(), student.getId());
        });
    }
    private GradeReviewResponse draft(Fixture f) {
        return grading.save(f.submissionId(), f.teacher().getUserId(), new BigDecimal("85.50"), "Good reasoning", null);
    }

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    no.hvl.dat250.lms.enrollment.service.EnrollmentService enrollmentSpy;
    @Autowired no.hvl.dat250.lms.content.service.ContentService contents;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired no.hvl.dat250.lms.submission.service.FileSubmissionService files;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage storage;

    private void independently(Runnable action) {
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> action.run());
    }

    private void afterOwnerLock(Fixture f, Runnable update) {
        org.mockito.Mockito.doAnswer(call -> {
            Object result = call.callRealMethod();
            independently(update);
            return result;
        }).when(enrollmentSpy).lockSubmissionOwner(f.enrollmentId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"create", "edit", "release"})
    void revocationAfterPrecheckIsRevalidatedForEveryGradeWrite(String operation) {
        var f = fixture();
        GradeReviewResponse existing = null;
        if (!operation.equals("create")) { existing = draft(f); }
        Long version = existing == null ? null : existing.version();
        afterOwnerLock(f, () -> jdbc.update("update enrollments set active=false where user_id=?",
            f.teacher().getUserId()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            if (operation.equals("release")) {
                grading.release(f.submissionId(), f.teacher().getUserId(), version);
            } else {
                grading.save(f.submissionId(), f.teacher().getUserId(), BigDecimal.TEN, "Must not save", version);
            }
        });
        if (existing == null) {
            assertTrue(grades.findBySubmission_Id(f.submissionId()).isEmpty());
        } else {
            var stored = grades.findBySubmission_Id(f.submissionId()).orElseThrow();
            assertEquals(new BigDecimal("85.50"), stored.getScore());
            assertNull(stored.getReleasedAt());
        }
    }

    @Test
    void teacherRoleChangeAfterPrecheckIsRevalidated() {
        var f = fixture();
        afterOwnerLock(f, () -> jdbc.update(
            "update enrollments set course_role='STUDENT' where user_id=?", f.teacher().getUserId()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> grading.save(f.submissionId(), f.teacher().getUserId(), BigDecimal.TEN, null, null));
        assertTrue(grades.findBySubmission_Id(f.submissionId()).isEmpty());
    }

    private Long submit(Fixture f, SubmissionType type) {
        if (type == SubmissionType.FILE) {
            return files.submitFile(f.contentId(), f.owner().getUserId(),
                new org.springframework.mock.web.MockMultipartFile("file", "report.pdf", "application/pdf",
                    "%PDF-1.7\nReport".getBytes())).id();
        }
        return submissions.submitIndividual(f.contentId(), f.owner().getUserId(), type,
            type == SubmissionType.TEXT ? "Answer" : "https://example.com/answer.git").getId();
    }

    private void rules(Fixture f, boolean past, boolean allowLate) {
        jdbc.update("update assignments set deadline=?, allow_late_submission=? where id=?",
            java.sql.Timestamp.from(Instant.parse(past ? "2000-01-01T00:00:00Z" : "2099-01-01T00:00:00Z")),
            allowLate, f.contentId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(SubmissionType.class)
    void closingDeadlineAfterPrecheckRejectsEveryChannel(SubmissionType type) {
        var f = fixture();
        rules(f, false, false);
        afterOwnerLock(f, () -> rules(f, true, false));
        assertThrows(no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException.class,
            () -> submit(f, type));
        assertEquals(1, submissions.findMySubmissionHistory(f.contentId(), f.owner().getUserId()).size());
        if (type == SubmissionType.FILE) {
            org.mockito.Mockito.verify(storage).delete(org.mockito.ArgumentMatchers.any());
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(SubmissionType.class)
    void refreshedDeadlineAndAllowLateDetermineAcceptedLateFlag(SubmissionType type) {
        var f = fixture();
        rules(f, false, false);
        afterOwnerLock(f, () -> rules(f, true, true));
        Long id = submit(f, type);
        assertTrue(submissions.findOwnSubmission(id, f.owner().getUserId()).isLate());
        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(SubmissionType.class)
    void disablingAllowLateAfterPrecheckRejectsEveryChannel(SubmissionType type) {
        var f = fixture();
        rules(f, true, true);
        afterOwnerLock(f, () -> rules(f, true, false));
        assertThrows(no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException.class,
            () -> submit(f, type));
    }

    // Real independent SQL writer: it must wait until the reader's transaction commits.
    private void assertWriterBlocked(Runnable lockedRead, Runnable update) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var reader = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                lockedRead.run();
                locked.countDown();
                try { assertTrue(finish.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
            }));
            try {
                assertTrue(locked.await(5, TimeUnit.SECONDS));
                CountDownLatch updating = new CountDownLatch(1);
                var writer = executor.submit(() -> independently(() -> { updating.countDown(); update.run(); }));
                assertTrue(updating.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> writer.get(250, TimeUnit.MILLISECONDS));
                finish.countDown();
                reader.get(5, TimeUnit.SECONDS);
                writer.get(5, TimeUnit.SECONDS);
            } finally { finish.countDown(); }
        }
    }

    @Test
    void assignmentSubtypeRulesCannotChangeUntilLockedTransactionCommits() throws Exception {
        var f = fixture();
        assertWriterBlocked(() -> contents.lockSubmissionContent(f.contentId()), () -> rules(f, true, false));
    }

    @Test
    void codingExerciseSubtypeRulesCannotChangeUntilLockedTransactionCommits() throws Exception {
        var f = fixture();
        Long id = new TransactionTemplate(transactions).execute(status -> {
            var assignment = entities.find(Assignment.class, f.contentId());
            return save(new no.hvl.dat250.lms.content.domain.CodingExercise(assignment.getModule(),
                "Code", 1, "Implement", "https://example.com/starter.git", null, true)).getId();
        });
        assertWriterBlocked(() -> contents.lockSubmissionContent(id), () -> jdbc.update(
            "update coding_exercises set allow_late_submission=false where id=?", id));
    }

    @Test
    void teacherRevocationWaitsUntilGradeAuthorizationTransactionCommits() throws Exception {
        var f = fixture();
        Long courseId = new TransactionTemplate(transactions).execute(status ->
            entities.find(Enrollment.class, f.enrollmentId()).getCourse().getId());
        assertWriterBlocked(() -> {
            enrollmentSpy.lockSubmissionOwner(f.enrollmentId());
            enrollmentSpy.lockActiveTeacher(f.teacher().getUserId(), courseId);
        }, () -> jdbc.update("update enrollments set active=false where user_id=?", f.teacher().getUserId()));
    }
}
