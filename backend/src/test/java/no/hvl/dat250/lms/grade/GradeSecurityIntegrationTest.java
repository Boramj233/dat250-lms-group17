package no.hvl.dat250.lms.grade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @Autowired ContentService contents;

    private <T> T save(T entity) { entities.persist(entity); entities.flush(); return entity; }
    private User user() {
        return save(new User("grade_" + UUID.randomUUID() + "@example.com", "Test", "User"));
    }
    private record Fixture(User owner, User teacher, User outsider, User inactive,
                           User otherStudent, Long contentId, Long submissionId, Long enrollmentId) {}
    private Fixture fixture() {
        return fixture(false);
    }
    private Fixture fixture(boolean coding) {
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
            Content content = coding
                ? new CodingExercise(module, "Code", 0, "Implement", "https://example.com/starter.git", null, false)
                : new Assignment(module, "Report", 0, "Write", null, false);
            save(content);
            Submission submission = save(new Submission(content, student, SubmissionType.TEXT,
                "Answer", Instant.parse("2026-01-01T12:00:00Z"), false));
            return new Fixture(owner, teacher, outsider, inactive, other,
                content.getId(), submission.getId(), student.getId());
        });
    }
    private GradeReviewResponse draft(Fixture f) {
        return grading.save(f.submissionId(), f.teacher().getUserId(), new BigDecimal("85.50"), "Good reasoning", null, true);
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void undecidedDraftCannotReleaseAndBothDecisionsCanRelease(boolean coding) throws Exception {
        for (boolean passed : new boolean[]{true, false}) {
            var f = fixture(coding);
            String base = "/api/submissions/" + f.submissionId() + "/grade";
            String token = TestTokens.bearer(f.teacher());
            mvc.perform(put(base).header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"score\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passed").isEmpty());
            mvc.perform(post(base + "/release").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isBadRequest());
            var unchanged = grading.review(f.submissionId(), f.teacher().getUserId());
            assertNull(unchanged.releasedAt());
            assertNull(unchanged.passed());
            assertEquals(0L, unchanged.version());
            mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.owner())))
                .andExpect(status().isNotFound());
            mvc.perform(put(base).header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"score\":100,\"version\":0,\"passed\":" + passed + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(passed));
            mvc.perform(post(base + "/release").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(passed))
                .andExpect(jsonPath("$.releasedAt").isNotEmpty());
            mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.owner())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(passed));
        }
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void saveEditReleaseAndStudentVisibility(boolean coding) throws Exception {
        var f = fixture(coding);
        String base = "/api/submissions/" + f.submissionId() + "/grade";
        String teacherToken = TestTokens.bearer(f.teacher());
        mvc.perform(get("/api/contents/{id}/submissions/review", f.contentId())
                .header("Authorization", teacherToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(f.submissionId()));
        mvc.perform(get("/api/submissions/{id}/review", f.submissionId())
                .header("Authorization", teacherToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.payload").value("Answer"));
        mvc.perform(get("/api/submissions/{id}/review/history", f.submissionId())
                .header("Authorization", teacherToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(f.submissionId()));
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
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":90,\"feedback\":\"Final\",\"version\":0,\"passed\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(post(base + "/release").header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.releasedAt").isNotEmpty());
        mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.owner())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(90))
            .andExpect(jsonPath("$.feedback").value("Final"))
            .andExpect(jsonPath("$.version").doesNotExist())
            .andExpect(jsonPath("$.gradedByEnrollmentId").doesNotExist());
        assertTrue(contents.isCompleted(f.contentId(), f.owner().getUserId()));
        mvc.perform(get(base).header("Authorization", TestTokens.bearer(f.otherStudent())))
            .andExpect(status().isNotFound());
        mvc.perform(put(base).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":95,\"version\":2}"))
            .andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void permissionsAreCourseScopedOnAllTeacherEndpoints(boolean coding) throws Exception {
        var f = fixture(coding);
        draft(f);
        String base = "/api/submissions/" + f.submissionId() + "/grade";
        for (User denied : new User[]{f.owner(), f.otherStudent(), f.outsider(), f.inactive()}) {
            String token = TestTokens.bearer(denied);
            mvc.perform(get("/api/contents/{id}/submissions/review", f.contentId())
                .header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/submissions/{id}/review", f.submissionId())
                .header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/submissions/{id}/review/history", f.submissionId())
                .header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get(base + "/review").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(put(base).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":50,\"version\":0}")).andExpect(status().isForbidden());
            mvc.perform(post(base + "/release").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}")).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/contents/{id}/submissions/review", f.contentId()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/submissions/{id}/review", f.submissionId()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/submissions/{id}/review/history", f.submissionId()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get(base)).andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/review")).andExpect(status().isUnauthorized());
        mvc.perform(put(base).contentType(MediaType.APPLICATION_JSON).content("{\"score\":50}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(base + "/release").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void rejectsInvalidScoresFeedbackAndStaleVersions(boolean coding) throws Exception {
        var f = fixture(coding);
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

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void resubmissionDoesNotInheritGradeAndHistoricalReleaseSurvivesWithdrawal(boolean coding) {
        var f = fixture(coding);
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

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void oldUnreleasedGradeCannotBePublishedAfterResubmission(boolean coding) {
        var f = fixture(coding);
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

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void concurrentCreateHasOneWinnerAndConcurrentReleaseIsIdempotent(boolean coding) throws Exception {
        var f = fixture(coding);
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

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void concurrentEditsWithSameVersionCannotOverwriteEachOther(boolean coding) throws Exception {
        var f = fixture(coding);
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
        Long pageId = new TransactionTemplate(transactions).execute(status -> {
            Assignment assignment = entities.find(Assignment.class, f.contentId());
            return save(new StaticPage(assignment.getModule(), "Page", 1, "Read")).getId();
        });
        mvc.perform(get("/api/contents/{id}/submissions/review", pageId).header("Authorization", token))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void submissionOwnerLockMakesGradingObserveAConcurrentResubmission(boolean coding) throws Exception {
        var f = fixture(coding);
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
