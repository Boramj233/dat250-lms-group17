package no.hvl.dat250.lms.submission.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import no.hvl.dat250.lms.user.domain.User;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters + HTTP/MockMvc + JPA commits + SeaweedFS; no outer test transaction. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:file_upload_it;DB_CLOSE_DELAY=-1",
        // Also verify that preflight entities cached by OSIV cannot bypass the recheck.
        "spring.jpa.open-in-view=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileSubmissionUploadIntegrationTest {

    private static final String ACCESS = "test" + UUID.randomUUID().toString().replace("-", "");
    private static final String SECRET = UUID.randomUUID().toString().replace("-", "");
    private static final String BUCKET = "lms-submissions";
    private static final byte[] PDF = "%PDF-1.7\nUpload integration test\n".getBytes(StandardCharsets.UTF_8);
    private static final GenericContainer<?> SEAWEED = new GenericContainer<>(
            "chrislusf/seaweedfs:4.53@sha256:ccad23e18639c8b98cee17e7100b95830a06077b56d8e5b006d6dcf8ec5e94a0")
            .withExposedPorts(8333)
            .withEnv("LMS_STORAGE_ACCESS_KEY", ACCESS)
            .withEnv("LMS_STORAGE_SECRET_KEY", SECRET)
            .withTmpFs(java.util.Map.of("/data", "rw"))
            .withCopyFileToContainer(MountableFile.forHostPath(System.getProperty("storage.init-script")), "/etc/lms/start.sh")
            .withCreateContainerCmdModifier(command -> command.withEntrypoint("/bin/sh", "/etc/lms/start.sh"))
            .waitingFor(Wait.forLogMessage(".*All enabled components are running and ready to use:.*\\n", 1)
                    .withStartupTimeout(Duration.ofSeconds(90)));

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        SEAWEED.start();
        registry.add("lms.submission.storage.enabled", () -> "true");
        registry.add("lms.submission.storage.endpoint", () -> "http://" + SEAWEED.getHost() + ":" + SEAWEED.getMappedPort(8333));
        registry.add("lms.submission.storage.access-key", () -> ACCESS);
        registry.add("lms.submission.storage.secret-key", () -> SECRET);
    }

    @AfterAll
    static void stopContainer() {
        SEAWEED.stop();
    }

    @Autowired MockMvc mvc;
    @Autowired EntityManager entities;
    @Autowired SubmissionRepository submissions;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired S3Client s3;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean SubmissionObjectStorage storage;
    @MockitoSpyBean SubmissionService submissionService;
    @LocalServerPort int port;

    private record Fixture(User user, Enrollment enrollment, Assignment assignment) {}

    private Fixture fixture(CourseRole role, boolean active, Instant deadline, boolean allowLate) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = new User("upload_" + UUID.randomUUID() + "@example.com", "Test", "Student");
            entities.persist(user);
            Course course = new Course("DAT250", "Upload test", Term.AUTUMN, 2026);
            entities.persist(course);
            Enrollment enrollment = new Enrollment(user, course, role);
            if (!active) {
                enrollment.deactivate();
            }
            entities.persist(enrollment);
            Module module = new Module(course, "Week 1", 0);
            entities.persist(module);
            Assignment assignment = new Assignment(module, "Report", 0, "Write a report", deadline, allowLate);
            entities.persist(assignment);
            entities.flush();
            return new Fixture(user, enrollment, assignment);
        });
    }

    private Fixture student() {
        return fixture(CourseRole.STUDENT, true, null, false);
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "report.pdf", "application/pdf", PDF);
    }

    private MockMultipartHttpServletRequestBuilder upload(Fixture fixture) {
        var request = multipart("/api/contents/{id}/submissions/file", fixture.assignment().getId());
        request.file(pdf());
        request.header("Authorization", TestTokens.bearer(fixture.user()));
        return request;
    }

    private Set<String> keys() {
        return s3.listObjectsV2(request -> request.bucket(BUCKET)).contents().stream()
                .map(object -> object.key()).collect(Collectors.toSet());
    }

    private Submission uploadAndFind(Fixture fixture) throws Exception {
        mvc.perform(upload(fixture)).andExpect(status().isCreated());
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM submissions WHERE content_id=?",
                Long.class, fixture.assignment().getId());
        return submissions.findById(id).orElseThrow();
    }

    @Test
    void realHttpDownloadReturnsExactBytesSafeUnicodeFilenameAndPrivateHeaders() throws Exception {
        Fixture fixture = student();
        var file = new MockMultipartFile("file", "../../报告\r\n.pdf", "application/pdf", PDF);
        mvc.perform(multipart("/api/contents/{id}/submissions/file", fixture.assignment().getId())
                .file(file).header("Authorization", TestTokens.bearer(fixture.user())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.file.downloadPath").isNotEmpty());
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM submissions WHERE content_id=?",
                Long.class, fixture.assignment().getId());
        URI uri = URI.create("http://localhost:" + port + "/api/submissions/" + id + "/file");

        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(uri)
                    .header("Authorization", TestTokens.bearer(fixture.user())).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertArrayEquals(PDF, response.body());
            assertEquals("application/pdf", response.headers().firstValue("Content-Type").orElseThrow());
            assertEquals(PDF.length, Long.parseLong(response.headers().firstValue("Content-Length").orElseThrow()));
            String cache = response.headers().firstValue("Cache-Control").orElseThrow();
            assertTrue(cache.contains("private"));
            assertTrue(cache.contains("no-store"));
            assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElseThrow());
            assertEquals("none", response.headers().firstValue("Accept-Ranges").orElseThrow());
            String disposition = response.headers().firstValue("Content-Disposition").orElseThrow();
            var parsed = org.springframework.http.ContentDisposition.parse(disposition);
            assertEquals("attachment", parsed.getType());
            assertEquals("报告.pdf", parsed.getFilename());
            assertFalse(disposition.contains("\r"));
            assertFalse(disposition.contains("\n"));

            var head = client.send(HttpRequest.newBuilder(uri)
                    .header("Authorization", TestTokens.bearer(fixture.user()))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, head.statusCode());
            assertEquals(0, head.body().length);
            assertEquals(Integer.toString(PDF.length), head.headers().firstValue("Content-Length").orElseThrow());

            // Resumable/range downloads are outside this MR; serve the complete attachment.
            var range = client.send(HttpRequest.newBuilder(uri)
                    .header("Authorization", TestTokens.bearer(fixture.user()))
                    .header("Range", "bytes=0-4").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, range.statusCode());
            assertArrayEquals(PDF, range.body());
        }
    }

    @Test
    void downloadRequiresJwtAndOwnerEvenForTeacherInTheSameCourse() throws Exception {
        Fixture fixture = student();
        Submission submission = uploadAndFind(fixture);
        Long id = submission.getId();
        mvc.perform(get("/api/submissions/{id}/file", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/submissions/{id}/file", id).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        User outsider = student().user();
        mvc.perform(get("/api/submissions/{id}/file", id).header("Authorization", TestTokens.bearer(outsider)))
            .andExpect(status().isNotFound());
        User teacher = new TransactionTemplate(transactionManager).execute(status -> {
            User user = new User("teacher_" + UUID.randomUUID() + "@example.com", "Test", "Teacher");
            entities.persist(user);
            Course course = entities.find(Enrollment.class, fixture.enrollment().getId()).getCourse();
            entities.persist(new Enrollment(user, course, CourseRole.TEACHER));
            entities.flush();
            return user;
        });
        mvc.perform(get("/api/submissions/{id}/file", id).header("Authorization", TestTokens.bearer(teacher)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/submissions/99999999/file").header("Authorization", TestTokens.bearer(fixture.user())))
            .andExpect(status().isNotFound());
        verify(storage, never()).get(any());
    }

    @Test
    void ownHistoricalDownloadSurvivesInactiveEnrollmentAndClosedDeadline() throws Exception {
        Fixture fixture = student();
        Submission submission = uploadAndFind(fixture);
        jdbc.update("UPDATE enrollments SET active=false WHERE id=?", fixture.enrollment().getId());
        jdbc.update("UPDATE assignments SET deadline=? WHERE id=?",
                java.sql.Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")), fixture.assignment().getId());
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            java.io.InputStream input = (java.io.InputStream) invocation.callRealMethod();
            return new java.io.FilterInputStream(input) {
                @Override
                public void close() throws java.io.IOException {
                    closed.set(true);
                    super.close();
                }
            };
        }).when(storage).get(any());

        mvc.perform(get("/api/submissions/{id}/file", submission.getId())
                .header("Authorization", TestTokens.bearer(fixture.user())))
            .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        assertTrue(closed.get());
    }

    @Test
    void nonFileSubmissionReturns404WithoutReadingStorage() throws Exception {
        Fixture fixture = student();
        for (SubmissionType type : new SubmissionType[] {SubmissionType.TEXT, SubmissionType.GIT_URL}) {
            Long id = new TransactionTemplate(transactionManager).execute(status -> {
                Submission submission = new Submission(
                    entities.find(Assignment.class, fixture.assignment().getId()),
                    entities.find(Enrollment.class, fixture.enrollment().getId()),
                    type, "existing payload", Instant.now(), false
                );
                entities.persist(submission);
                entities.flush();
                return submission.getId();
            });
            mvc.perform(get("/api/submissions/{id}/file", id)
                    .header("Authorization", TestTokens.bearer(fixture.user())))
                .andExpect(status().isNotFound());
        }
        verify(storage, never()).get(any());
    }

    @Test
    void missingObjectOrUnavailableStorageReturns503WithoutLeakingLocation() throws Exception {
        Fixture fixture = student();
        Submission submission = uploadAndFind(fixture);
        String key = submission.getFile().getObjectKey();
        // Remove only this test object's bytes, keeping its SQL reference to simulate storage loss.
        storage.delete(new SubmissionObjectStorage.ObjectReference(BUCKET, key));
        URI uri = URI.create("http://localhost:" + port + "/api/submissions/" + submission.getId() + "/file");
        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(uri)
                    .header("Authorization", TestTokens.bearer(fixture.user())).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Disposition").isEmpty());
            assertFalse(response.body().contains(key));
            assertFalse(response.body().contains(BUCKET));
        }
        doThrow(new SubmissionStorageException("simulated unavailable", null)).when(storage).get(any());
        mvc.perform(get("/api/submissions/{id}/file", submission.getId())
                .header("Authorization", TestTokens.bearer(fixture.user())))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void realUploadCreatesImmutableVersionsForTokenOwnerAndKeepsExactBytes() throws Exception {
        Fixture fixture = student();
        long before = submissions.count();
        mvc.perform(upload(fixture).param("userId", "999999"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("FILE"))
            .andExpect(jsonPath("$.file.name").value("report.pdf"))
            .andExpect(jsonPath("$.file.bucket").doesNotExist())
            .andExpect(jsonPath("$.file.objectKey").doesNotExist());
        mvc.perform(upload(fixture)).andExpect(status().isCreated());
        assertEquals(before + 2, submissions.count());
        var versions = submissions.findAll().stream()
                .filter(submission -> submission.getType() == SubmissionType.FILE)
                .filter(submission -> fixture.assignment().getId().equals(
                        jdbc.queryForObject("SELECT content_id FROM submissions WHERE id=?", Long.class, submission.getId())))
                .toList();
        assertEquals(2, versions.size());
        assertNotEquals(versions.get(0).getFile().getObjectKey(), versions.get(1).getFile().getObjectKey());
        for (Submission version : versions) {
            assertEquals(fixture.enrollment().getId(), jdbc.queryForObject(
                    "SELECT submitted_by_id FROM submissions WHERE id=?", Long.class, version.getId()));
            assertNull(version.getPayload());
            var metadata = version.getFile();
            try (var input = storage.get(new SubmissionObjectStorage.ObjectReference(BUCKET, metadata.getObjectKey()))) {
                assertArrayEquals(PDF, input.readAllBytes());
            }
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(PDF));
            assertEquals(hash, metadata.getSha256());
        }
    }

    @Test
    void jwtAndActiveStudentInTheAssignmentCourseAreRequired() throws Exception {
        Fixture owner = student();
        Set<String> before = keys();
        mvc.perform(multipart("/api/contents/{id}/submissions/file", owner.assignment().getId()).file(pdf()))
            .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/contents/{id}/submissions/file", owner.assignment().getId())
                .file(pdf()).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        Fixture outsider = student();
        mvc.perform(multipart("/api/contents/{id}/submissions/file", owner.assignment().getId())
                .file(pdf()).header("Authorization", TestTokens.bearer(outsider.user())))
            .andExpect(status().isForbidden());
        mvc.perform(upload(fixture(CourseRole.TEACHER, true, null, false))).andExpect(status().isForbidden());
        mvc.perform(upload(fixture(CourseRole.STUDENT, false, null, false))).andExpect(status().isForbidden());
        assertEquals(before, keys());
        verify(storage, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void closedDeadlineIsRejectedAndAllowedLateUploadIsMarked() throws Exception {
        Instant past = Instant.parse("2000-01-01T00:00:00Z");
        mvc.perform(upload(fixture(CourseRole.STUDENT, true, past, false)))
            .andExpect(status().isConflict());
        mvc.perform(upload(fixture(CourseRole.STUDENT, true, past, true)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(true));
    }

    @Test
    void malformedEmptyMultipleOrGroupFilesAreRejectedWithoutStorageWrites() throws Exception {
        Fixture fixture = student();
        String path = "/api/contents/" + fixture.assignment().getId() + "/submissions/file";
        String token = TestTokens.bearer(fixture.user());
        mvc.perform(multipart(path).header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(multipart(path).file(new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[0]))
                .header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(multipart(path).file(new MockMultipartFile("file", "report.pdf", "application/pdf", "fake".getBytes()))
                .header("Authorization", token)).andExpect(status().isUnsupportedMediaType());
        mvc.perform(multipart(path).file(new MockMultipartFile("file", "report.docx", "application/pdf", PDF))
                .header("Authorization", token)).andExpect(status().isUnsupportedMediaType());
        mvc.perform(upload(fixture).file(pdf())).andExpect(status().isBadRequest());
        mvc.perform(upload(fixture).file(new MockMultipartFile("extra", "report.pdf", "application/pdf", PDF)))
            .andExpect(status().isBadRequest());
        mvc.perform(upload(fixture).param("groupId", "123")).andExpect(status().isBadRequest());
        verify(storage, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void revokedEnrollmentDuringUploadIsRecheckedDespiteOpenEntityManagerInView() throws Exception {
        Fixture fixture = student();
        Set<String> before = keys();
        long count = submissions.count();
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            Object result = invocation.callRealMethod();
            jdbc.update("UPDATE enrollments SET active=false WHERE id=?", fixture.enrollment().getId());
            return result;
        }).when(storage).put(anyString(), any(), anyLong(), anyString());

        mvc.perform(upload(fixture)).andExpect(status().isForbidden());

        assertEquals(count, submissions.count());
        assertEquals(before, keys());
    }

    @Test
    void deadlineClosedDuringUploadDeletesObjectAndRejectsSubmission() throws Exception {
        Fixture fixture = student();
        Set<String> before = keys();
        long count = submissions.count();
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            jdbc.update("UPDATE assignments SET deadline=? WHERE id=?",
                    java.sql.Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")), fixture.assignment().getId());
            return result;
        }).when(storage).put(anyString(), any(), anyLong(), anyString());

        mvc.perform(upload(fixture)).andExpect(status().isConflict());

        assertEquals(count, submissions.count());
        assertEquals(before, keys());
    }

    @Test
    void filesRequireExistingAssignmentRatherThanCodingExercise() throws Exception {
        Fixture fixture = student();
        String token = TestTokens.bearer(fixture.user());
        mvc.perform(multipart("/api/contents/99999999/submissions/file").file(pdf())
                .header("Authorization", token)).andExpect(status().isNotFound());
        Long exerciseId = new TransactionTemplate(transactionManager).execute(status -> {
            Assignment assignment = entities.find(Assignment.class, fixture.assignment().getId());
            CodingExercise exercise = new CodingExercise(assignment.getModule(), "Code", 1,
                    "Implement", "https://example.com/starter.git", null, false);
            entities.persist(exercise);
            entities.flush();
            return exercise.getId();
        });
        mvc.perform(multipart("/api/contents/{id}/submissions/file", exerciseId).file(pdf())
                .header("Authorization", token)).andExpect(status().isBadRequest());
        verify(storage, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void sqlCommitPhaseFailureRollsBackRowAndDeletesRealObject() throws Exception {
        Fixture fixture = student();
        Set<String> before = keys();
        long count = submissions.count();
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new DataIntegrityViolationException("Injected commit-phase failure");
                }
            });
            return result;
        }).when(submissionService).saveFileSubmission(anyLong(), anyLong(), any());

        mvc.perform(upload(fixture)).andExpect(status().isServiceUnavailable());

        assertEquals(count, submissions.count());
        assertEquals(before, keys());
    }

    @Test
    void lostUploadReplyCleansRealObjectWithoutCreatingSqlRow() throws Exception {
        Fixture fixture = student();
        Set<String> before = keys();
        long count = submissions.count();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new SubmissionStorageException("simulated lost upload reply", null);
        }).when(storage).put(anyString(), any(), anyLong(), anyString());

        mvc.perform(upload(fixture)).andExpect(status().isServiceUnavailable());

        assertEquals(count, submissions.count());
        assertEquals(before, keys());
    }

    @Test
    void actualHttpMultipartParserRejectsFileOverTenMiB() throws Exception {
        Fixture fixture = student();
        Set<String> before = keys();
        long count = submissions.count();
        String boundary = "testboundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] prefix = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"report.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
        var body = HttpRequest.BodyPublishers.concat(
            HttpRequest.BodyPublishers.ofByteArray(prefix),
            HttpRequest.BodyPublishers.ofByteArray(new byte[10 * 1024 * 1024 + 1]),
            HttpRequest.BodyPublishers.ofByteArray(suffix)
        );
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/api/contents/" + fixture.assignment().getId() + "/submissions/file"))
                .header("Authorization", TestTokens.bearer(fixture.user()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(30)).POST(body).build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(413, response.statusCode(), response.body());
        }
        assertEquals(count, submissions.count());
        assertEquals(before, keys());
    }
    @Test
    void courseTeacherDownloadsHistoricalFileThroughReviewEndpoint() throws Exception {
        Fixture fixture = student();
        Submission original = uploadAndFind(fixture);
        uploadAndFind(fixture);
        User teacher = new TransactionTemplate(transactionManager).execute(status -> {
            User user = new User("teacher_" + UUID.randomUUID() + "@example.com", "Test", "Teacher");
            entities.persist(user);
            Course course = entities.find(Assignment.class, fixture.assignment().getId())
                .getModule().getCourse();
            entities.persist(new Enrollment(user, course, CourseRole.TEACHER));
            entities.flush();
            return user;
        });
        String path = "/api/submissions/" + original.getId() + "/review/file";
        mvc.perform(get(path).header("Authorization", TestTokens.bearer(teacher)))
            .andExpect(status().isOk()).andExpect(content().bytes(PDF))
            .andExpect(header().string("Cache-Control", "no-store, private"));
        mvc.perform(head(path).header("Authorization", TestTokens.bearer(teacher)))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[0]))
            .andExpect(header().longValue("Content-Length", PDF.length));
        // The existing owner-only endpoint deliberately remains owner-only.
        mvc.perform(get("/api/submissions/{id}/file", original.getId())
                .header("Authorization", TestTokens.bearer(teacher)))
            .andExpect(status().isNotFound());
        clearInvocations(storage);
        mvc.perform(get(path).header("Authorization", TestTokens.bearer(fixture.user())))
            .andExpect(status().isForbidden());
        verify(storage, never()).get(any());
    }

}
