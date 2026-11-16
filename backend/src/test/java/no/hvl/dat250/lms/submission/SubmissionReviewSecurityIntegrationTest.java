package no.hvl.dat250.lms.submission;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters and JPA; storage is mocked, no outer transaction or OSIV. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:review_security_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionReviewSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager entities;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean SubmissionObjectStorage storage;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User("review_" + UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(User owner, User teacher, User inactiveTeacher, User outsider,
                           Assignment assignment, Long oldId, Long latestId, String key) {}

    private Fixture fixture() {
        return new TransactionTemplate(transactions).execute(status -> {
            Course course = save(new Course("DAT250", "Review", Term.AUTUMN, 2026));
            Course other = save(new Course("Other", "Other", Term.AUTUMN, 2026));
            User owner = user();
            User teacher = user();
            User inactiveTeacher = user();
            User outsider = user();
            Enrollment student = save(new Enrollment(owner, course, CourseRole.STUDENT));
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            Enrollment inactive = save(new Enrollment(inactiveTeacher, course, CourseRole.TEACHER));
            inactive.deactivate();
            save(new Enrollment(outsider, other, CourseRole.TEACHER));
            Module module = save(new Module(course, "Week", 0));
            Assignment assignment = save(new Assignment(module, "Report", 0, "Write", null, false));
            // Equal timestamps verify the deterministic ID tie-breaker.
            Instant time = Instant.parse("2026-11-01T12:00:00Z");
            Submission old = save(new Submission(assignment, student, SubmissionType.TEXT, "Old", time, false));
            String key = "submissions/" + UUID.randomUUID();
            Submission latest = save(Submission.file(assignment, student,
                new SubmissionFileMetadata("private", key, "report.pdf", "application/pdf", 5, "a".repeat(64)),
                time, true));
            student.deactivate(); // Historical review does not depend on the student's current role.
            return new Fixture(owner, teacher, inactiveTeacher, outsider, assignment, old.getId(), latest.getId(), key);
        });
    }

    @Test
    void teacherReviewsLatestAndHistoryWithSafeMetadata() throws Exception {
        var f = fixture();
        String token = TestTokens.bearer(f.teacher());
        mvc.perform(get("/api/contents/{id}/submissions/review", f.assignment().getId())
                .header("Authorization", token).param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(f.latestId()))
            .andExpect(jsonPath("$.content[0].studentUserId").value(f.owner().getUserId()));
        mvc.perform(get("/api/submissions/{id}/review", f.latestId()).header("Authorization", token))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store, private"))
            .andExpect(jsonPath("$.late").value(true))
            .andExpect(jsonPath("$.file.downloadPath").value("/api/submissions/" + f.latestId() + "/review/file"))
            .andExpect(jsonPath("$.file.bucket").doesNotExist())
            .andExpect(jsonPath("$.file.objectKey").doesNotExist());
        mvc.perform(get("/api/submissions/{id}/review/history", f.oldId()).header("Authorization", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(f.latestId()))
            .andExpect(jsonPath("$[1].payload").value("Old"));
        mvc.perform(get("/api/contents/{id}/submissions/review", f.assignment().getId())
                .header("Authorization", token).param("size", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void latestListPaginatesAcrossStudentsAndExcludesOtherAssignments() throws Exception {
        var f = fixture();
        Long secondId = new TransactionTemplate(transactions).execute(status -> {
            Assignment assignment = entities.find(Assignment.class, f.assignment().getId());
            User student = user();
            Enrollment enrollment = save(new Enrollment(student, assignment.getModule().getCourse(), CourseRole.STUDENT));
            Submission second = save(new Submission(assignment, enrollment, SubmissionType.GIT_URL,
                "https://example.com/answer.git", Instant.parse("2026-11-02T12:00:00Z"), false));
            Assignment other = save(new Assignment(assignment.getModule(), "Other", 1, "Write", null, false));
            save(new Submission(other, enrollment, SubmissionType.TEXT, "Other answer", Instant.now(), false));
            return second.getId();
        });
        mvc.perform(get("/api/contents/{id}/submissions/review", f.assignment().getId())
                .header("Authorization", TestTokens.bearer(f.teacher())).param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(secondId));
        mvc.perform(get("/api/contents/{id}/submissions/review", f.assignment().getId())
                .header("Authorization", TestTokens.bearer(f.teacher())).param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(f.latestId()));
    }

    @Test
    void allReviewEndpointsEnforceCourseTeacherBeforeStorageAccess() throws Exception {
        var f = fixture();
        String[] paths = {
            "/api/contents/" + f.assignment().getId() + "/submissions/review",
            "/api/submissions/" + f.latestId() + "/review",
            "/api/submissions/" + f.latestId() + "/review/history",
            "/api/submissions/" + f.latestId() + "/review/file"
        };
        for (String path : paths) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (User denied : new User[]{f.owner(), f.inactiveTeacher(), f.outsider()}) {
                mvc.perform(get(path).header("Authorization", TestTokens.bearer(denied)))
                    .andExpect(status().isForbidden());
            }
        }
        mvc.perform(head(paths[3]).header("Authorization", TestTokens.bearer(f.outsider())))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/submissions/{id}/file", f.latestId())
                .header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isNotFound());
        verifyNoInteractions(storage);
    }

    @Test
    void teacherDownloadAndHeadReuseStorageAndCloseStreams() throws Exception {
        var f = fixture();
        var reference = new SubmissionObjectStorage.ObjectReference("private", f.key());
        AtomicBoolean closed = new AtomicBoolean();
        when(storage.get(reference)).thenAnswer(invocation -> new ByteArrayInputStream("%PDF-".getBytes()) {
            @Override public void close() { closed.set(true); }
        });
        String path = "/api/submissions/" + f.latestId() + "/review/file";
        mvc.perform(get(path).header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isOk()).andExpect(content().bytes("%PDF-".getBytes()))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        assertTrue(closed.get());
        closed.set(false);
        when(storage.get(reference)).thenAnswer(invocation -> new java.io.InputStream() {
            @Override public int read() { throw new AssertionError("HEAD must not read"); }
            @Override public void close() { closed.set(true); }
        });
        mvc.perform(head(path).header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[0]));
        assertTrue(closed.get());
        mvc.perform(get("/api/submissions/{id}/review/file", f.oldId())
                .header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/submissions/99999999/review").header("Authorization", TestTokens.bearer(f.teacher())))
            .andExpect(status().isNotFound());
    }
}
