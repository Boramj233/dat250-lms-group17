package no.hvl.dat250.lms.grade;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.grade.service.GradingService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.user.domain.User;
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

/** Scheduled release (SaveGradeRequest.visibleFrom) and POST /api/contents/{id}/grades/release. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:grade_release_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.jpa.open-in-view=false",
    // The test runs the release itself, so the scheduler does not race it.
    "lms.grades.release-scheduler.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradeReleaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager entities;
    @Autowired PlatformTransactionManager transactions;
    @Autowired GradingService grading;
    @Autowired ContentService contents;

    private <T> T save(T entity) { entities.persist(entity); entities.flush(); return entity; }
    private User user() { return save(new User("release_" + UUID.randomUUID() + "@example.com", "Test", "User")); }

    private record Fixture(User teacher, User outsider, User first, User second, User third,
                           Long contentId, Long firstSubmission, Long secondSubmission, Long thirdSubmission) {}

    private Fixture fixture() {
        return new TransactionTemplate(transactions).execute(status -> {
            Course course = save(new Course("DAT250", "Release", Term.AUTUMN, 2026));
            User teacher = user(), outsider = user(), first = user(), second = user(), third = user();
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            Enrollment a = save(new Enrollment(first, course, CourseRole.STUDENT));
            Enrollment b = save(new Enrollment(second, course, CourseRole.STUDENT));
            Enrollment c = save(new Enrollment(third, course, CourseRole.STUDENT));
            save(new Enrollment(outsider, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            Assignment assignment = save(new Assignment(module, "Report", 0, "Write", null, false));
            Instant at = Instant.parse("2026-01-01T12:00:00Z");
            Submission s1 = save(new Submission(assignment, a, SubmissionType.TEXT, "One", at, false));
            Submission s2 = save(new Submission(assignment, b, SubmissionType.TEXT, "Two", at, false));
            Submission s3 = save(new Submission(assignment, c, SubmissionType.TEXT, "Three", at, false));
            return new Fixture(teacher, outsider, first, second, third,
                assignment.getId(), s1.getId(), s2.getId(), s3.getId());
        });
    }

    private String grade(Long submissionId, User teacher, String body) throws Exception {
        return mvc.perform(put("/api/submissions/{id}/grade", submissionId)
                .header("Authorization", TestTokens.bearer(teacher))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void scheduledGradeIsHiddenUntilItsTimeAndThenReleased() throws Exception {
        Fixture f = fixture();
        Instant later = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        mvc.perform(put("/api/submissions/{id}/grade", f.firstSubmission())
                .header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":80,\"passed\":true,\"visibleFrom\":\"" + later + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visibleFrom").value(later.toString()))
            .andExpect(jsonPath("$.releasedAt").isEmpty());
        mvc.perform(get("/api/submissions/{id}/grade", f.firstSubmission())
                .header("Authorization", TestTokens.bearer(f.first())))
            .andExpect(status().isNotFound());
        assertEquals(0, grading.releaseDue());

        // Moved into the past: released by the next run.
        new TransactionTemplate(transactions).executeWithoutResult(status ->
            entities.createQuery("update Grade g set g.visibleFrom = :past where g.submission.id = :id")
                .setParameter("past", Instant.now().minus(1, ChronoUnit.MINUTES))
                .setParameter("id", f.firstSubmission()).executeUpdate());
        assertEquals(1, grading.releaseDue());
        mvc.perform(get("/api/submissions/{id}/grade", f.firstSubmission())
                .header("Authorization", TestTokens.bearer(f.first())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(80));
        assertTrue(contents.isCompleted(f.contentId(), f.first().getUserId()));
    }

    @Test
    void aVisibleDateInThePastReleasesAtOnceAndNeedsADecision() throws Exception {
        Fixture f = fixture();
        mvc.perform(put("/api/submissions/{id}/grade", f.firstSubmission())
                .header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":50,\"visibleFrom\":\"2026-01-01T00:00:00Z\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/submissions/{id}/grade", f.firstSubmission())
                .header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":50,\"passed\":false,\"visibleFrom\":\"2026-01-01T00:00:00Z\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.releasedAt").isNotEmpty());
    }

    @Test
    void releaseAllReleasesDecidedGradesAndCountsUndecidedOnes() throws Exception {
        Fixture f = fixture();
        grade(f.firstSubmission(), f.teacher(), "{\"score\":90,\"passed\":true}");
        grade(f.secondSubmission(), f.teacher(), "{\"score\":40}");
        // The third submission has no grade at all.

        mvc.perform(post("/api/contents/{id}/grades/release", f.contentId())
                .header("Authorization", TestTokens.bearer(f.outsider())))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/contents/{id}/grades/release", f.contentId()))
            .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/contents/{id}/grades/release", f.contentId())
                .header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.released").value(1))
            .andExpect(jsonPath("$.undecided").value(1));
        mvc.perform(get("/api/submissions/{id}/grade", f.firstSubmission())
                .header("Authorization", TestTokens.bearer(f.first())))
            .andExpect(status().isOk());
        mvc.perform(get("/api/submissions/{id}/grade", f.secondSubmission())
                .header("Authorization", TestTokens.bearer(f.second())))
            .andExpect(status().isNotFound());

        // Again: nothing more to release.
        mvc.perform(post("/api/contents/{id}/grades/release", f.contentId())
                .header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.released").value(0))
            .andExpect(jsonPath("$.undecided").value(1));
    }

    @Test
    void releaseAllIsOnlyForGradableContent() throws Exception {
        Fixture f = fixture();
        mvc.perform(post("/api/contents/{id}/grades/release", 999_999L)
                .header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isNotFound());
    }
}
