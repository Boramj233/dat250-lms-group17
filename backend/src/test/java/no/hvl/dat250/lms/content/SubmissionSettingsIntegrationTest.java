package no.hvl.dat250.lms.content;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.service.*;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.grade.service.GradingService;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters, committed data, fixed server time; storage network is mocked. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:submission_settings_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionSettingsIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private AssignmentService assignments;
    @Autowired private CodingExerciseService codingExercises;
    @Autowired private ContentService contents;
    @Autowired private SubmissionService submissions;
    @Autowired private GradingService grades;
    @MockitoBean(name = "submissionClock") private Clock clock;
    @MockitoBean private SubmissionObjectStorage storage;

    @BeforeEach
    void fixedServerTime() {
        when(clock.instant()).thenReturn(NOW);
    }
    private <T> T tx(Supplier<T> action) {
        return new TransactionTemplate(transactions).execute(status -> action.get());
    }
    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }
    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }
    private record Fixture(boolean coding, Long courseId, Long contentId, Long pageId, Long groupId,
                           User teacher, User student, User member) {
    }
    private Fixture fixture(boolean coding) {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Submission settings", Term.AUTUMN, 2026));
            User teacher = user(), student = user(), member = user();
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            save(new Enrollment(student, course, CourseRole.STUDENT));
            save(new Enrollment(member, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            Content content = coding
                ? new CodingExercise(module, "Original", 0, "Instructions", "https://example.com/starter", "code", NOW.plusSeconds(60), false)
                : new Assignment(module, "Original", 0, "Instructions", NOW.plusSeconds(60), false);
            content.setRequiredForModuleCompletion(true);
            if (content instanceof Assignment assignment) assignment.setMandatoryForExam(true);
            save(content);
            StaticPage page = save(new StaticPage(module, "Page", 1, "Body"));
            Group group = new Group(1, course);
            group.addGroupMember(student); group.addGroupMember(member); save(group);
            return new Fixture(coding, course.getId(), content.getId(), page.getId(), group.getGroupId(), teacher, student, member);
        });
    }
    private String path(boolean coding, Long id) {
        return (coding ? "/api/coding-exercises/" : "/api/assignments/") + id;
    }
    private String settingsPath(Fixture fixture) {
        return path(fixture.coding(), fixture.contentId()) + "/submission-settings";
    }
    private String body(Instant deadline, boolean allowLate) {
        return "{\"deadline\":" + (deadline == null ? "null" : "\"" + deadline + "\"")
            + ",\"allowLateSubmission\":" + allowLate + "}";
    }
    private void update(Fixture fixture, Instant deadline, boolean allowLate) throws Exception {
        var result = mvc.perform(put(settingsPath(fixture)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(body(deadline, allowLate)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(fixture.contentId()))
            .andExpect(jsonPath("$.allowLateSubmission").value(allowLate));
        if (deadline == null) result.andExpect(jsonPath("$.deadline").isEmpty());
        else result.andExpect(jsonPath("$.deadline").value(deadline.toString()));
    }
    private ResultActions submit(Fixture fixture, SubmissionType type, boolean group) throws Exception {
        if (type == SubmissionType.FILE) {
            var request = multipart("/api/contents/{id}/submissions/file", fixture.contentId())
                .file(new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.7\nReport".getBytes()));
            if (group) request.param("groupId", fixture.groupId().toString());
            return mvc.perform(request.header("Authorization", TestTokens.bearer(fixture.student())));
        }
        String payload = type == SubmissionType.TEXT ? "Answer" : "https://example.com/answer.git";
        return mvc.perform(post("/api/contents/{id}/submissions", fixture.contentId())
            .header("Authorization", TestTokens.bearer(fixture.student()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"" + type + "\",\"payload\":\"" + payload + "\""
                + (group ? ",\"groupId\":" + fixture.groupId() : "") + "}"));
    }
    private void assertProtectedFields(Fixture fixture) {
        tx(() -> {
            Content content = entities.find(Content.class, fixture.contentId());
            assertEquals("Original", content.getTitle());
            assertEquals(0, content.getPosition());
            assertEquals(fixture.courseId(), content.getModule().getCourse().getId());
            assertTrue(content.isRequiredForModuleCompletion());
            if (fixture.coding()) {
                CodingExercise coding = entities.find(CodingExercise.class, fixture.contentId());
                assertEquals("Instructions", coding.getInstructions());
                assertEquals("code", coding.getStarterCode());
                assertEquals("https://example.com/starter", coding.getStarterRepositoryUrl());
            } else {
                Assignment assignment = entities.find(Assignment.class, fixture.contentId());
                assertEquals("Instructions", assignment.getInstructions());
                assertTrue(assignment.isMandatoryForExam());
            }
            return null;
        });
    }
    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void teacherCanReplaceExtendShortenAndClearSettings(boolean coding) throws Exception {
        Fixture fixture = fixture(coding);
        for (Instant deadline : List.of(NOW.plusSeconds(3600), NOW.minusSeconds(3600), NOW)) {
            update(fixture, deadline, true);
            mvc.perform(get(path(coding, fixture.contentId())).header("Authorization", TestTokens.bearer(fixture.student())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deadline").value(deadline.toString()))
                .andExpect(jsonPath("$.allowLateSubmission").value(true));
        }
        update(fixture, null, false);
        update(fixture, null, false);
        mvc.perform(put(settingsPath(fixture)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"allowLateSubmission\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deadline").isEmpty());
        assertProtectedFields(fixture);
    }
    @ParameterizedTest(name = "coding={0}, channel={1}, group={2}")
    @CsvSource({"false,TEXT,false", "false,GIT_URL,false", "false,FILE,false",
        "false,TEXT,true", "false,GIT_URL,true", "false,FILE,true",
        "true,TEXT,false", "true,GIT_URL,false", "true,TEXT,true", "true,GIT_URL,true"})
    void newRulesApplyToEverySupportedSubmissionChannel(boolean coding, SubmissionType type, boolean group) throws Exception {
        Fixture fixture = fixture(coding);
        update(fixture, NOW.plusSeconds(1), false);
        submit(fixture, type, group).andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(false));
        update(fixture, NOW, false);
        submit(fixture, type, group).andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(false));
        update(fixture, NOW.minusSeconds(1), false);
        submit(fixture, type, group).andExpect(status().isConflict());
        update(fixture, NOW.minusSeconds(1), true);
        var accepted = submit(fixture, type, group).andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(true));
        if (group) accepted.andExpect(jsonPath("$.members.length()").value(2));
        update(fixture, null, false);
        submit(fixture, type, group).andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(false));
        assertEquals(4, submissions.findMySubmissionHistory(fixture.contentId(), fixture.student().getUserId()).size());
        if (group) assertEquals(4, submissions.findMySubmissionHistory(fixture.contentId(), fixture.member().getUserId()).size());
        assertProtectedFields(fixture);
    }
    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void permissionsValidationAndResourceTypesAreEnforced(boolean coding) throws Exception {
        Fixture fixture = fixture(coding), other = fixture(coding);
        User inactive = tx(() -> {
            User teacher = user();
            Enrollment enrollment = save(new Enrollment(teacher, entities.find(Course.class, fixture.courseId()), CourseRole.TEACHER));
            enrollment.deactivate();
            return teacher;
        });
        String path = settingsPath(fixture);
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body(null, false)))
            .andExpect(status().isUnauthorized());
        for (User denied : List.of(fixture.student(), other.teacher(), inactive)) {
            mvc.perform(put(path).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(body(null, false)))
                .andExpect(status().isForbidden());
        }
        for (String invalid : List.of("{}", "{\"deadline\":null}", "{\"allowLateSubmission\":null}",
                "{\"deadline\":\"invalid-time\",\"allowLateSubmission\":false}")) {
            mvc.perform(put(path).header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
        }
        for (Long id : List.of(Long.MAX_VALUE, fixture.pageId())) {
            mvc.perform(put(path(coding, id) + "/submission-settings")
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(body(null, false)))
                .andExpect(status().isNotFound());
        }
        mvc.perform(get(path(coding, fixture.contentId())).header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deadline").value(NOW.plusSeconds(60).toString()))
            .andExpect(jsonPath("$.allowLateSubmission").value(false));
        assertProtectedFields(fixture);
    }
    private Content updateFromService(Fixture fixture, Instant deadline, boolean allowLate, Long teacherUserId) {
        return fixture.coding()
            ? codingExercises.updateSubmissionSettings(fixture.contentId(), deadline, allowLate, teacherUserId)
            : assignments.updateSubmissionSettings(fixture.contentId(), deadline, allowLate, teacherUserId);
    }
    @ParameterizedTest(name = "coding={0}, group={1}")
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void historicalLateGradesAndCompletionAreNotRewritten(boolean coding, boolean group) throws Exception {
        Fixture fixture = fixture(coding);
        update(fixture, NOW.minusSeconds(1), true);
        Submission old = submissions.submit(fixture.contentId(), fixture.student().getUserId(), SubmissionType.TEXT,
            "Historical answer", group ? fixture.groupId() : null);
        assertTrue(old.isLate());
        var draft = grades.save(old.getId(), fixture.teacher().getUserId(), BigDecimal.ZERO, "Feedback", null, true);
        grades.release(old.getId(), fixture.teacher().getUserId(), draft.version());
        var released = grades.ownReleased(old.getId(), fixture.student().getUserId());
        update(fixture, NOW.plusSeconds(60), false);
        submit(fixture, SubmissionType.TEXT, group).andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(false));
        assertEquals(released, grades.ownReleased(old.getId(), fixture.student().getUserId()));
        assertTrue(contents.isCompleted(fixture.contentId(), fixture.student().getUserId()));
        mvc.perform(get("/api/submissions/{id}", old.getId()).header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.late").value(true))
            .andExpect(jsonPath("$.payload").value("Historical answer"));
        if (group) {
            assertEquals(released, grades.ownReleased(old.getId(), fixture.member().getUserId()));
            assertTrue(contents.isCompleted(fixture.contentId(), fixture.member().getUserId()));
        }
        assertProtectedFields(fixture);
    }
    @ParameterizedTest(name = "coding={0}, group={1}")
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void settingsEditWaitsForAcceptedSubmissionAndOnlyAffectsNextSubmission(boolean coding, boolean group) throws Exception {
        Fixture fixture = fixture(coding);
        CountDownLatch accepted = new CountDownLatch(1), finish = new CountDownLatch(1), editing = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var acceptance = pool.submit(() -> tx(() -> {
                Long id = submissions.submit(fixture.contentId(), fixture.student().getUserId(), SubmissionType.TEXT,
                    "Accepted before close", group ? fixture.groupId() : null).getId();
                accepted.countDown();
                try { assertTrue(finish.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException exception) { throw new RuntimeException(exception); }
                return id;
            }));
            try {
                assertTrue(accepted.await(5, TimeUnit.SECONDS));
                var edit = pool.submit(() -> {
                    editing.countDown();
                    return updateFromService(fixture, NOW.minusSeconds(1), false, fixture.teacher().getUserId());
                });
                assertTrue(editing.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> edit.get(250, TimeUnit.MILLISECONDS));
                finish.countDown();
                Long id = acceptance.get(5, TimeUnit.SECONDS);
                edit.get(5, TimeUnit.SECONDS);
                assertFalse(submissions.findOwnSubmission(id, fixture.student().getUserId()).isLate());
            } finally { finish.countDown(); }
        }
        submit(fixture, SubmissionType.TEXT, group).andExpect(status().isConflict());
    }
    @ParameterizedTest(name = "group={0}")
    @ValueSource(booleans = {false, true})
    void fileFinalAcceptanceRechecksSettingsChangedDuringUpload(boolean group) throws Exception {
        Fixture fixture = fixture(false);
        doAnswer(call -> {
            updateFromService(fixture, NOW.minusSeconds(1), false, fixture.teacher().getUserId());
            return new SubmissionObjectStorage.ObjectReference("test", call.getArgument(0));
        }).when(storage).put(any(), any(), anyLong(), any());
        submit(fixture, SubmissionType.FILE, group).andExpect(status().isConflict());
        assertTrue(submissions.findMySubmissionHistory(fixture.contentId(), fixture.student().getUserId()).isEmpty());
        verify(storage).delete(any());
    }
    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void serviceAlsoChecksTeacherAfterAcquiringContentLock(boolean coding) {
        Fixture fixture = fixture(coding);
        assertThrows(AccessDeniedException.class,
            () -> updateFromService(fixture, null, false, fixture.student().getUserId()));
    }
}
