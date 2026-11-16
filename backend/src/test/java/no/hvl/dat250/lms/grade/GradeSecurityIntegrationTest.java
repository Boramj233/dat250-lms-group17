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
    "spring.datasource.url=jdbc:h2:mem:grade_security_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradeSecurityIntegrationTest {
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

    @Test
    void saveEditReleaseAndStudentVisibility() throws Exception {
        var f = fixture();
        String base = "/api/submissions/" + f.submissionId() + "/grade";
        mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":85.50,\"feedback\":\"First\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.releasedAt").isEmpty());
        mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.owner())))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.feedback").doesNotExist());
        // Existing student submission endpoints cannot leak feedback either.
        mvc.perform(get("/api/submissions/{id}", f.submissionId()).header("Authorization", TestTokens.bearer(f.owner())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.grade").doesNotExist())
            .andExpect(jsonPath("$.feedback").doesNotExist());
        mvc.perform(get("/api/contents/{id}/submissions/me", f.contentId()).header("Authorization", TestTokens.bearer(f.owner())))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].feedback").doesNotExist());
        mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":90,\"feedback\":\"Final\",\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(post(base + "/release").header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.releasedAt").isNotEmpty());
        mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.owner())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(90))
            .andExpect(jsonPath("$.feedback").value("Final"))
            .andExpect(jsonPath("$.version").doesNotExist())
            .andExpect(jsonPath("$.gradedByEnrollmentId").doesNotExist());
        mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.otherStudent())))
            .andExpect(status().isNotFound());
        mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":95,\"version\":2}"))
            .andExpect(status().isConflict());
    }

    @Test
    void permissionsAreCourseScopedOnAllTeacherEndpoints() throws Exception {
        var f = fixture();
        draft(f);
        String base = "/api/submissions/" + f.submissionId() + "/grade";
        for (User denied : new User[]{f.owner(), f.otherStudent(), f.outsider(), f.inactive()}) {
            String token = TestTokens.bearer(denied);
            mvc.perform(get(base + "/review").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(put(base).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":50,\"version\":0}")).andExpect(status().isForbidden());
            mvc.perform(post(base + "/release").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}")).andExpect(status().isForbidden());
        }
        mvc.perform(get(base)).andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/review")).andExpect(status().isUnauthorized());
        mvc.perform(put(base).contentType(MediaType.APPLICATION_JSON).content("{\"score\":50}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(base + "/release").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidScoresFeedbackAndStaleVersions() throws Exception {
        var f = fixture();
        String base = "/api/submissions/" + f.submissionId() + "/grade";
        for (String score : new String[]{"-1", "100.01", "12.345", "null"}) {
            mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"score\":" + score + "}"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":50,\"feedback\":\"" + "x".repeat(10001) + "\"}"))
            .andExpect(status().isBadRequest());
        draft(f);
        mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":50}"))
            .andExpect(status().isConflict());
        mvc.perform(post(base + "/release").header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":9}"))
            .andExpect(status().isConflict());
        mvc.perform(post(base + "/release").header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resubmissionDoesNotInheritGradeAndHistoricalReleaseSurvivesWithdrawal() {
        var f = fixture();
        var grade = draft(f);
        var released = grading.release(f.submissionId(), f.teacher().getUserId(), grade.version());
        Long newer = submissions.submitIndividual(f.contentId(), f.owner().getUserId(), SubmissionType.TEXT, "New answer").getId();
        assertThrows(java.util.NoSuchElementException.class, () -> grading.ownReleased(newer, f.owner().getUserId()));
        assertThrows(GradeConflictException.class, () -> grading.save(f.submissionId(), f.teacher().getUserId(),
            BigDecimal.TEN, null, released.version()));
        assertEquals(released, grading.release(f.submissionId(), f.teacher().getUserId(), grade.version()));
        new TransactionTemplate(transactions).executeWithoutResult(status ->
            entities.find(Enrollment.class, f.enrollmentId()).deactivate());
        assertEquals(new BigDecimal("85.50"), grading.ownReleased(f.submissionId(), f.owner().getUserId()).score());
        assertEquals(0L, grading.save(newer, f.teacher().getUserId(), BigDecimal.ZERO, null, null).version());
    }

    @Test
    void oldUnreleasedGradeCannotBePublishedAfterResubmission() {
        var f = fixture();
        var grade = draft(f);
        submissions.submitIndividual(f.contentId(), f.owner().getUserId(), SubmissionType.TEXT, "New answer");
        assertThrows(GradeConflictException.class, () -> grading.release(f.submissionId(), f.teacher().getUserId(), grade.version()));
        assertThrows(GradeConflictException.class, () -> grading.save(f.submissionId(), f.teacher().getUserId(), BigDecimal.TEN, null, grade.version()));
        assertNull(grading.review(f.submissionId(), f.teacher().getUserId()).releasedAt());
    }

    private <T> java.util.List<T> concurrently(Callable<T> operation) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<T> task = () -> { start.await(); return operation.call(); };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            return java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentCreateHasOneWinnerAndConcurrentReleaseIsIdempotent() throws Exception {
        var f = fixture();
        var created = concurrently(() -> {
            try { draft(f); return true; }
            catch (GradeConflictException conflict) { return false; }
        });
        assertEquals(1, created.stream().filter(Boolean::booleanValue).count());
        var grade = grading.review(f.submissionId(), f.teacher().getUserId());
        var releases = concurrently(() -> grading.release(f.submissionId(), f.teacher().getUserId(), grade.version()));
        assertEquals(releases.get(0), releases.get(1));
        assertNotNull(releases.get(0).releasedAt());
    }

    @Test
    void concurrentEditsWithSameVersionCannotOverwriteEachOther() throws Exception {
        var f = fixture();
        var grade = draft(f);
        var results = concurrently(() -> {
            try {
                grading.save(f.submissionId(), f.teacher().getUserId(), BigDecimal.TEN, "Revised", grade.version());
                return true;
            } catch (GradeConflictException conflict) { return false; }
        });
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1L, grading.review(f.submissionId(), f.teacher().getUserId()).version());
    }

    @Test
    void missingGradesAndUnsupportedContentAreNotGraded() throws Exception {
        var f = fixture();
        String token = TestTokens.bearer(f.teacher());
        mvc.perform(post("/api/submissions/{id}/grade/release", f.submissionId())
                .header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/submissions/99999999/grade/review").header("Authorization", token))
            .andExpect(status().isNotFound());
        Long coding = new TransactionTemplate(transactions).execute(status -> {
            Assignment assignment = entities.find(Assignment.class, f.contentId());
            var exercise = save(new no.hvl.dat250.lms.content.domain.CodingExercise(
                assignment.getModule(), "Code", 1, "Implement", "https://example.com/starter.git", null, false));
            return save(new Submission(exercise, entities.find(Enrollment.class, f.enrollmentId()),
                SubmissionType.GIT_URL, "https://example.com/answer.git", Instant.now(), false)).getId();
        });
        mvc.perform(put("/api/submissions/{id}/grade", coding).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":50}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void submissionOwnerLockMakesGradingObserveAConcurrentResubmission() throws Exception {
        var f = fixture();
        var grade = draft(f);
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var submit = executor.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                submissions.submitIndividual(f.contentId(), f.owner().getUserId(), SubmissionType.TEXT, "Concurrent new answer");
                accepted.countDown();
                try { assertTrue(allowCommit.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
                return true;
            }));
            assertTrue(accepted.await(10, TimeUnit.SECONDS));
            CountDownLatch tryingRelease = new CountDownLatch(1);
            var release = executor.submit(() -> {
                tryingRelease.countDown();
                return assertThrows(GradeConflictException.class,
                    () -> grading.release(f.submissionId(), f.teacher().getUserId(), grade.version()));
            });
            assertTrue(tryingRelease.await(10, TimeUnit.SECONDS));
            allowCommit.countDown();
            assertTrue(submit.get(15, TimeUnit.SECONDS));
            release.get(15, TimeUnit.SECONDS);
        } finally { allowCommit.countDown(); }
        assertNull(grading.review(f.submissionId(), f.teacher().getUserId()).releasedAt());
    }
}
