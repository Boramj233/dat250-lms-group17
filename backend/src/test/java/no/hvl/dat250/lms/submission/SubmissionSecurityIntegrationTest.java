package no.hvl.dat250.lms.submission;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real security filters, JWT converter, services and H2. TestJwtDecoderConfig trusts
 * TestTokens' signing key instead of Keycloak's JWK endpoint; this is not a Keycloak E2E test.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:submission_security_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubmissionSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager entities;
    @Autowired SubmissionRepository submissions;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User("submission_" + UUID.randomUUID() + "@example.com", "Test", "Student"));
    }

    private Course course() {
        return save(new Course("DAT250", "Security test", Term.AUTUMN, 2026));
    }

    private record Fixture(User user, Course course, Enrollment enrollment, Assignment assignment) {}

    private Fixture fixture(Instant deadline, boolean allowLate) {
        User user = user();
        Course course = course();
        Enrollment enrollment = save(new Enrollment(user, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week 1", 0));
        Assignment assignment = save(new Assignment(module, "Report", 0, "Explain JPA", deadline, allowLate));
        return new Fixture(user, course, enrollment, assignment);
    }

    private MockHttpServletRequestBuilder submit(Content content, User user) {
        return post("/api/contents/{id}/submissions", content.getId())
            .header("Authorization", TestTokens.bearer(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"type":"TEXT","payload":"Private answer"}
                """);
    }

    @Test
    void allSubmissionEndpointsRequireBearerToken() throws Exception {
        for (var request : new MockHttpServletRequestBuilder[] {
            post("/api/contents/1/submissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"payload\":\"Answer\"}"),
            get("/api/submissions/1"), get("/api/contents/1/submissions/me")
        }) {
            mvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(header().doesNotExist("Location"));
        }
        assertEquals(0, submissions.count());
    }

    @Test
    void invalidTokenIsRejectedBeforeSubmission() throws Exception {
        var f = fixture(null, false);
        mvc.perform(post("/api/contents/{id}/submissions", f.assignment().getId())
                .header("Authorization", "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"payload\":\"Answer\"}"))
            .andExpect(status().isUnauthorized());
        assertEquals(0, submissions.count());
    }

    @Test
    void validStudentCreatesSubmissionForTokenOwnerAndReadsOwnHistory() throws Exception {
        var f = fixture(Instant.parse("2099-01-01T00:00:00Z"), false);
        // An untrusted query parameter cannot change the submitting identity.
        User other = user();
        mvc.perform(submit(f.assignment(), f.user()).param("userId", other.getUserId().toString()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(false))
            .andExpect(header().doesNotExist("Set-Cookie"));
        entities.flush();
        entities.clear();
        Submission saved = submissions.findAll().getFirst();
        assertEquals(f.enrollment().getId(), saved.getSubmittedBy().getId());
        assertEquals("Private answer", saved.getPayload());
        assertEquals(SubmissionType.TEXT, saved.getType());
        assertNotNull(saved.getSubmittedAt());
        mvc.perform(get("/api/submissions/{id}", saved.getId())
                .header("Authorization", TestTokens.bearer(f.user())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.payload").value("Private answer"));
        // Real HTTP requests do not share a persistence context. Clear the lazy
        // Content proxy from the preceding detail request in this test transaction.
        entities.clear();
        mvc.perform(get("/api/contents/{id}/submissions/me", f.assignment().getId())
                .header("Authorization", TestTokens.bearer(f.user())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(saved.getId()));
    }

    @Test
    void codingExerciseAcceptsGitUrl() throws Exception {
        var f = fixture(null, false);
        var exercise = save(new CodingExercise(f.assignment().getModule(), "Code", 1,
            "Implement", "https://example.com/starter.git", null, false));
        mvc.perform(post("/api/contents/{id}/submissions", exercise.getId())
                .header("Authorization", TestTokens.bearer(f.user()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"GIT_URL\",\"payload\":\"https://example.com/answer.git\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("GIT_URL"));
        assertEquals(1, submissions.count());
    }

    @Test
    void studentInAnotherCourseCannotSubmit() throws Exception {
        var f = fixture(null, false);
        User outsider = user();
        save(new Enrollment(outsider, course(), CourseRole.STUDENT));
        mvc.perform(submit(f.assignment(), outsider)).andExpect(status().isForbidden());
        assertEquals(0, submissions.count());
    }

    @Test
    void inactiveStudentCannotSubmit() throws Exception {
        var f = fixture(null, false);
        f.enrollment().deactivate();
        entities.flush();
        mvc.perform(submit(f.assignment(), f.user())).andExpect(status().isForbidden());
        assertEquals(0, submissions.count());
    }

    @Test
    void courseTeacherCannotSubmitAsStudent() throws Exception {
        var f = fixture(null, false);
        User teacher = user();
        save(new Enrollment(teacher, f.course(), CourseRole.TEACHER));
        mvc.perform(submit(f.assignment(), teacher)).andExpect(status().isForbidden());
        assertEquals(0, submissions.count());
    }

    @Test
    void nonOwnerCannotReadDetailOrSeeOthersHistory() throws Exception {
        var f = fixture(null, false);
        User other = user();
        save(new Enrollment(other, f.course(), CourseRole.STUDENT));
        mvc.perform(submit(f.assignment(), f.user())).andExpect(status().isCreated());
        Long id = submissions.findAll().getFirst().getId();
        mvc.perform(get("/api/contents/{id}/submissions/me", f.assignment().getId())
                .header("Authorization", TestTokens.bearer(other)))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/submissions/{id}", id)
                .header("Authorization", TestTokens.bearer(other)))
            .andExpect(status().isNotFound());
    }

    @Test
    void closedDeadlineReturns409AndDoesNotSave() throws Exception {
        var f = fixture(Instant.parse("2000-01-01T00:00:00Z"), false);
        mvc.perform(submit(f.assignment(), f.user())).andExpect(status().isConflict());
        assertEquals(0, submissions.count());
    }

    @Test
    void allowedLateSubmissionIsFlaggedAndPersisted() throws Exception {
        var f = fixture(Instant.parse("2000-01-01T00:00:00Z"), true);
        mvc.perform(submit(f.assignment(), f.user())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.late").value(true));
        entities.flush();
        entities.clear();
        assertTrue(submissions.findAll().getFirst().isLate());
    }

    @Test
    void missingContentReturns404() throws Exception {
        User user = user();
        mvc.perform(post("/api/contents/999999/submissions")
                .header("Authorization", TestTokens.bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TEXT\",\"payload\":\"Answer\"}"))
            .andExpect(status().isNotFound());
    }
    @Test
    void jsonEndpointRejectsFileTypeAndForgedStorageMetadata() throws Exception {
        var f = fixture(null, false);
        for (String body : new String[] {
                """
                {"type":"FILE","payload":"forged-key"}
                """,
                """
                {"type":"FILE","payload":"forged-key",
                 "file":{"bucket":"lms-submissions","objectKey":"someone-elses-file"}}
                """
        }) {
            mvc.perform(post("/api/contents/{id}/submissions", f.assignment().getId())
                    .header("Authorization", TestTokens.bearer(f.user()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0, submissions.count());
    }

    @Test
    void ownFileMetadataIsReadableWithoutLeakingStorageLocation() throws Exception {
        var f = fixture(null, false);
        var metadata = new no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata(
                "lms-submissions", "submissions/private-key", "report.pdf", "application/pdf", 123, "a".repeat(64));
        var submission = save(Submission.file(f.assignment(), f.enrollment(), metadata, Instant.now(), false));
        entities.clear();
        mvc.perform(get("/api/submissions/{id}", submission.getId())
                .header("Authorization", TestTokens.bearer(f.user())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("FILE"))
            .andExpect(jsonPath("$.payload").isEmpty())
            .andExpect(jsonPath("$.file.name").value("report.pdf"))
            .andExpect(jsonPath("$.file.mediaType").value("application/pdf"))
            .andExpect(jsonPath("$.file.sizeBytes").value(123))
            .andExpect(jsonPath("$.file.bucket").doesNotExist())
            .andExpect(jsonPath("$.file.objectKey").doesNotExist())
            .andExpect(jsonPath("$.file.sha256").doesNotExist());
        User other = user();
        mvc.perform(get("/api/submissions/{id}", submission.getId())
                .header("Authorization", TestTokens.bearer(other)))
            .andExpect(status().isNotFound());
    }

}
