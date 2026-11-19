package no.hvl.dat250.lms.content;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.content.service.ContentCompletionService;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.grade.service.GradingService;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters, committed fixtures and OSIV disabled. No live storage or development database. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:content_visibility_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentVisibilityIntegrationTest {
    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private static final Instant FUTURE = NOW.plusSeconds(3600);
    private static final Instant PAST = NOW.minusSeconds(3600);
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private ContentService contents;
    @Autowired private ContentCompletionService completion;
    @Autowired private SubmissionService submissions;
    @Autowired private GradingService grades;
    @Autowired private QuizAttemptRepository attempts;
    @MockitoBean(name = "submissionClock") private Clock clock;
    @MockitoBean private SubmissionObjectStorage storage;

    @BeforeEach
    void fixedSubmissionTime() {
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
    private record Fixture(Long courseId, Long moduleId, Long pageId, Long assignmentId, Long codingId,
                           Long quizId, Long questionId, Long optionId, Long wrongOptionId, Long groupId,
                           User teacher, User student, User member) {
        List<Long> contentIds() { return List.of(pageId, assignmentId, codingId, quizId); }
    }
    private Fixture fixture() {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Visibility", Term.AUTUMN, 2026));
            User teacher = user(), student = user(), member = user();
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            save(new Enrollment(student, course, CourseRole.STUDENT));
            save(new Enrollment(member, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            StaticPage page = save(new StaticPage(module, "Page", 0, "Body"));
            Assignment assignment = save(new Assignment(module, "Assignment", 1, "Instructions", null, false));
            CodingExercise coding = save(new CodingExercise(module, "Coding", 2, "Instructions", "https://example.com/repo", "code", null, false));
            Quiz quiz = save(new Quiz(module, "Quiz", 3, 3));
            QuizQuestion question = save(new QuizQuestion(quiz, "Choose", 0, "Consider the question"));
            ChoiceOption option = save(new ChoiceOption(question, "Correct", 0, true));
            ChoiceOption wrong = save(new ChoiceOption(question, "Wrong", 1, false));
            for (Content content : List.of(page, assignment, coding, quiz)) content.setRequiredForModuleCompletion(true);
            Group group = new Group(1, course, null);
            group.addGroupMember(student);
            group.addGroupMember(member);
            save(group);
            return new Fixture(course.getId(), module.getId(), page.getId(), assignment.getId(), coding.getId(),
                quiz.getId(), question.getId(), option.getId(), wrong.getId(), group.getGroupId(), teacher, student, member);
        });
    }
    private String settings(Long id) { return "/api/contents/" + id + "/visibility"; }
    private String list(Fixture fixture) {
        return "/api/courses/" + fixture.courseId() + "/modules/" + fixture.moduleId() + "/contents";
    }
    private String detail(Fixture fixture, Long id) {
        if (id.equals(fixture.pageId())) return "/api/contents/" + id;
        if (id.equals(fixture.assignmentId())) return "/api/assignments/" + id;
        if (id.equals(fixture.codingId())) return "/api/coding-exercises/" + id;
        return "/api/quizzes/" + id;
    }
    private ResultActions getAs(String path, User user) throws Exception {
        return mvc.perform(get(path).header("Authorization", TestTokens.bearer(user)));
    }
    private ResultActions configure(Fixture fixture, Long id, String body) throws Exception {
        return mvc.perform(put(settings(id)).header("Authorization", TestTokens.bearer(fixture.teacher()))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private void setFrom(Fixture fixture, Long id, Instant from) throws Exception {
        configure(fixture, id, "{\"visibleFrom\":" + (from == null ? "null" : "\"" + from + "\"") + "}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.contentId").value(id));
    }
    private String windowBody(Instant from, Instant until) {
        return "{\"visibleFrom\":" + (from == null ? "null" : "\"" + from + "\"")
            + ",\"visibleUntil\":" + (until == null ? "null" : "\"" + until + "\"") + "}";
    }
    private void setWindow(Fixture fixture, Long id, Instant from, Instant until) throws Exception {
        configure(fixture, id, windowBody(from, until)).andExpect(status().isOk())
            .andExpect(jsonPath("$.contentId").value(id));
    }
    private void hide(Fixture fixture, Long id, boolean closeEnd) throws Exception {
        if (closeEnd) setWindow(fixture, id, null, PAST);
        else setFrom(fixture, id, FUTURE);
    }
    private ResultActions submit(Fixture fixture, Long id, String type, boolean group) throws Exception {
        return mvc.perform(post("/api/contents/{id}/submissions", id)
            .header("Authorization", TestTokens.bearer(fixture.student())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"" + type + "\",\"payload\":\"https://example.com/answer\""
                + (group ? ",\"groupId\":" + fixture.groupId() : "") + "}"));
    }
    private ResultActions file(Fixture fixture, boolean group) throws Exception {
        var request = multipart("/api/contents/{id}/submissions/file", fixture.assignmentId())
            .file(new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.7\nReport".getBytes()));
        if (group) request.param("groupId", fixture.groupId().toString());
        return mvc.perform(request.header("Authorization", TestTokens.bearer(fixture.student())));
    }
    private ResultActions quizSubmit(Fixture fixture) throws Exception {
        return quizSubmit(fixture, true);
    }
    private ResultActions quizSubmit(Fixture fixture, boolean correct) throws Exception {
        return mvc.perform(post("/api/quizzes/{id}/attempts", fixture.quizId())
            .header("Authorization", TestTokens.bearer(fixture.student())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"answers\":[{\"questionId\":" + fixture.questionId() + ",\"selectedOptionIds\":["
                + (correct ? fixture.optionId() : fixture.wrongOptionId()) + "]}]}"));
    }
    private ResultActions mark(Fixture fixture, boolean completed) throws Exception {
        return mvc.perform(put("/api/contents/{id}/completion", fixture.pageId())
            .header("Authorization", TestTokens.bearer(fixture.student())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"completed\":" + completed + "}"));
    }

    @Test
    void timeBoundariesAndChangingStartUseSimpleWindowRules() {
        StaticPage page = new StaticPage(null, "Page", 0, "Body");
        assertTrue(page.isVisibleAt(NOW)); // Existing null values keep their meaning.
        page.changeVisibleFrom(NOW);
        assertFalse(page.isVisibleAt(NOW.minusNanos(1)));
        assertTrue(page.isVisibleAt(NOW));
        assertTrue(page.isVisibleAt(NOW.plusNanos(1)));
        page.changeVisibility(NOW, FUTURE);
        assertTrue(page.isVisibleAt(FUTURE.minusNanos(1)));
        assertFalse(page.isVisibleAt(FUTURE));
        assertFalse(page.isVisibleAt(FUTURE.plusNanos(1)));
        assertThrows(IllegalArgumentException.class, () -> page.changeVisibleFrom(FUTURE));
        assertThrows(IllegalArgumentException.class, () -> page.changeVisibleFrom(FUTURE.plusNanos(1)));
        assertEquals(NOW, page.getVisibleFrom());
        page.changeVisibleFrom(null);
        assertTrue(page.isVisibleAt(PAST));
        assertFalse(page.isVisibleAt(FUTURE)); // Clearing start never clears the existing end.
    }

    @Test
    void allFourTypesCanBeConfiguredClearedAndPreviewedWithoutChangingInternalQueries() throws Exception {
        Fixture fixture = fixture();
        getAs(list(fixture), fixture.student()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        for (Long id : fixture.contentIds()) {
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isOk());
            configure(fixture, id, "{\"visibleFrom\":\"" + FUTURE + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.visibleFrom").value(FUTURE.toString()));
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isForbidden());
            getAs(detail(fixture, id), fixture.teacher()).andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleFrom").value(FUTURE.toString()));
            assertFalse(contents.isCompleted(id, fixture.student().getUserId()));
            assertTrue(tx(() -> contents.requireContent(id).isRequiredForModuleCompletion()));
        }
        getAs(list(fixture) + "?userId=" + fixture.teacher().getUserId(), fixture.student())
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        getAs(list(fixture), fixture.teacher()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].visibleFrom").value(FUTURE.toString()));
        assertEquals(4, contents.findContentsByModule(fixture.courseId(), fixture.moduleId()).size());
        for (Long id : fixture.contentIds()) {
            setFrom(fixture, id, PAST);
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isOk());
            setFrom(fixture, id, null);
            assertNull(tx(() -> contents.requireContent(id).getVisibleFrom()));
            setFrom(fixture, id, FUTURE);
            configure(fixture, id, "{}").andExpect(status().isOk());
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleFrom").isEmpty());
        }
    }

    @Test
    void teacherCanScheduleCloseAndReopenAllFourTypesThroughTheSameEndpoint() throws Exception {
        Fixture fixture = fixture();
        for (Long id : fixture.contentIds()) {
            setWindow(fixture, id, FUTURE, FUTURE.plusSeconds(3600));
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isForbidden());
            getAs(detail(fixture, id), fixture.teacher()).andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleUntil").value(FUTURE.plusSeconds(3600).toString()));
            // Both new bounds are validated together, without an intermediate invalid window.
            setWindow(fixture, id, FUTURE.plusSeconds(7200), FUTURE.plusSeconds(10800));
            setWindow(fixture, id, PAST, FUTURE);
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleFrom").value(PAST.toString()))
                .andExpect(jsonPath("$.visibleUntil").value(FUTURE.toString()));
            setWindow(fixture, id, null, PAST);
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isForbidden());
            assertTrue(tx(() -> contents.requireContent(id).isRequiredForModuleCompletion()));
        }
        getAs(list(fixture), fixture.student()).andExpect(status().isOk()).andExpect(content().json("[]"));
        getAs(list(fixture), fixture.teacher()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].visibleUntil").value(PAST.toString()));
        assertEquals(4, contents.findContentsByModule(fixture.courseId(), fixture.moduleId()).size());
        for (Long id : fixture.contentIds()) {
            setWindow(fixture, id, null, null);
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleFrom").isEmpty()).andExpect(jsonPath("$.visibleUntil").isEmpty());
        }
        getAs(list(fixture), fixture.student()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void omittedEndPreservesItWhileExplicitNullClearsItAndStartSemanticsStayCompatible() throws Exception {
        Fixture fixture = fixture();
        Long id = fixture.pageId();
        setWindow(fixture, id, PAST, FUTURE);
        setFrom(fixture, id, NOW);
        assertEquals(FUTURE, tx(() -> contents.requireContent(id).getVisibleUntil()));
        configure(fixture, id, "{}").andExpect(status().isOk());
        assertNull(tx(() -> contents.requireContent(id).getVisibleFrom()));
        assertEquals(FUTURE, tx(() -> contents.requireContent(id).getVisibleUntil()));
        setWindow(fixture, id, PAST, FUTURE);
        configure(fixture, id, "{\"visibleUntil\":null}").andExpect(status().isOk())
            .andExpect(jsonPath("$.visibleFrom").isEmpty()).andExpect(jsonPath("$.visibleUntil").isEmpty());
        assertNull(tx(() -> contents.requireContent(id).getVisibleUntil()));
        // The DTO's presence flag is not a client-editable property.
        setWindow(fixture, id, PAST, FUTURE);
        configure(fixture, id, "{\"visibleUntilProvided\":true}").andExpect(status().isOk())
            .andExpect(jsonPath("$.visibleUntil").value(FUTURE.toString()));
    }

    @Test
    void invalidWindowsAndEndTimestampsLeaveBothStoredBoundsUnchanged() throws Exception {
        Fixture fixture = fixture();
        for (Long id : fixture.contentIds()) {
            setWindow(fixture, id, PAST, FUTURE);
            for (String invalid : List.of(windowBody(FUTURE, PAST), windowBody(NOW, NOW),
                    "{\"visibleUntil\":\"not-a-date\"}", "{\"visibleUntil\":\"2026-12-01T10:00:00\"}")) {
                configure(fixture, id, invalid).andExpect(status().isBadRequest());
                assertEquals(PAST, tx(() -> contents.requireContent(id).getVisibleFrom()));
                assertEquals(FUTURE, tx(() -> contents.requireContent(id).getVisibleUntil()));
            }
        }
        configure(fixture, Long.MAX_VALUE, windowBody(null, PAST)).andExpect(status().isNotFound());
        assertThrows(AccessDeniedException.class, () -> contents.updateVisibilityWindow(
            fixture.pageId(), fixture.student().getUserId(), null, PAST));
    }

    @Test
    void explicitEndCannotBypassCourseTeacherAuthorization() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        String body = windowBody(null, PAST);
        for (Long id : fixture.contentIds()) {
            mvc.perform(put(settings(id)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
            for (User denied : List.of(fixture.student(), fixture.member(), other.teacher(), other.student())) {
                mvc.perform(put(settings(id)).header("Authorization", TestTokens.bearer(denied))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
            }
            assertNull(tx(() -> contents.requireContent(id).getVisibleUntil()));
        }
        tx(() -> {
            entities.createQuery("select e from Enrollment e where e.user.userId=:user and e.course.id=:course", Enrollment.class)
                .setParameter("user", fixture.teacher().getUserId()).setParameter("course", fixture.courseId())
                .getSingleResult().deactivate();
            return null;
        });
        configure(fixture, fixture.pageId(), body).andExpect(status().isForbidden());
    }

    @Test
    void configurationRequiresActualCourseTeacherAndRejectsInvalidDates() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        for (Long id : fixture.contentIds()) {
            mvc.perform(put(settings(id)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
            for (User denied : List.of(fixture.student(), fixture.member(), other.teacher(), other.student())) {
                mvc.perform(put(settings(id)).header("Authorization", TestTokens.bearer(denied))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visibleFrom\":\"" + FUTURE + "\"}"))
                    .andExpect(status().isForbidden());
            }
            configure(fixture, id, "{\"visibleFrom\":\"not-a-date\"}").andExpect(status().isBadRequest());
            assertNull(tx(() -> contents.requireContent(id).getVisibleFrom()));
        }
        configure(fixture, Long.MAX_VALUE, "{}").andExpect(status().isNotFound());
        assertThrows(AccessDeniedException.class,
            () -> contents.updateVisibility(fixture.pageId(), fixture.student().getUserId(), FUTURE));
        tx(() -> {
            entities.createQuery("select e from Enrollment e where e.user.userId=:user and e.course.id=:course", Enrollment.class)
                .setParameter("user", fixture.teacher().getUserId()).setParameter("course", fixture.courseId())
                .getSingleResult().deactivate();
            return null;
        });
        configure(fixture, fixture.pageId(), "{}").andExpect(status().isForbidden());
        getAs(list(fixture), other.teacher()).andExpect(status().isForbidden());
        getAs(detail(fixture, fixture.quizId()), other.teacher()).andExpect(status().isForbidden());
    }

    @Test
    void startOnlyUpdatesPreserveAndEnforceExistingEndBoundary() throws Exception {
        Fixture fixture = fixture();
        tx(() -> {
            for (Long id : fixture.contentIds()) ReflectionTestUtils.setField(entities.find(Content.class, id), "visibleUntil", PAST);
            return null;
        });
        for (Long id : fixture.contentIds()) {
            configure(fixture, id, "{\"visibleFrom\":\"" + PAST + "\"}").andExpect(status().isBadRequest());
            setFrom(fixture, id, null);
            assertEquals(PAST, tx(() -> contents.requireContent(id).getVisibleUntil()));
            getAs(detail(fixture, id), fixture.student()).andExpect(status().isForbidden());
            getAs(detail(fixture, id), fixture.teacher()).andExpect(status().isOk());
        }
        getAs(list(fixture), fixture.student()).andExpect(status().isOk()).andExpect(content().json("[]"));
        quizSubmit(fixture).andExpect(status().isForbidden());
        mark(fixture, true).andExpect(status().isForbidden());
        for (Long id : List.of(fixture.assignmentId(), fixture.codingId())) {
            submit(fixture, id, "TEXT", false).andExpect(status().isForbidden());
            submit(fixture, id, "TEXT", true).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pageCompletionIsRetainedButCannotBeChangedOrReadThroughHiddenPageEndpoint(boolean closeEnd) throws Exception {
        Fixture fixture = fixture();
        mark(fixture, true).andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        hide(fixture, fixture.pageId(), closeEnd);
        mark(fixture, true).andExpect(status().isForbidden());
        mark(fixture, false).andExpect(status().isForbidden());
        getAs("/api/contents/" + fixture.pageId() + "/completion", fixture.student()).andExpect(status().isForbidden());
        assertTrue(contents.isCompleted(fixture.pageId(), fixture.student().getUserId()));
        assertTrue(completion.status(fixture.pageId(), fixture.student().getUserId()).completed());
        setWindow(fixture, fixture.pageId(), null, null);
        getAs("/api/contents/" + fixture.pageId() + "/completion", fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void hiddenQuizRejectsNewAttemptsButRetainsHistoryHintsAndCompletion(boolean closeEnd) throws Exception {
        Fixture fixture = fixture();
        hide(fixture, fixture.quizId(), closeEnd);
        quizSubmit(fixture).andExpect(status().isForbidden());
        assertFalse(attempts.existsByQuiz_Id(fixture.quizId()));
        setWindow(fixture, fixture.quizId(), null, null);
        quizSubmit(fixture, false).andExpect(status().isCreated()).andExpect(jsonPath("$.correctCount").value(0));
        Long attemptId = tx(() -> entities.createQuery("select a.id from QuizAttempt a where a.quiz.id=:quiz", Long.class)
            .setParameter("quiz", fixture.quizId()).getSingleResult());
        quizSubmit(fixture).andExpect(status().isCreated()).andExpect(jsonPath("$.correctCount").value(1));
        hide(fixture, fixture.quizId(), closeEnd);
        quizSubmit(fixture).andExpect(status().isForbidden());
        String path = "/api/quizzes/" + fixture.quizId() + "/attempts";
        getAs(path + "/me/count", fixture.student()).andExpect(status().isOk()).andExpect(jsonPath("$.usedAttempts").value(2));
        getAs(path + "/me", fixture.student()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        getAs(path + "/" + attemptId, fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.answers[0].correct").value(false))
            .andExpect(jsonPath("$.hints[0].hint").value("Consider the question"));
        getAs(path + "/" + attemptId + "/review", fixture.teacher()).andExpect(status().isOk());
        getAs("/api/quizzes/" + fixture.quizId() + "/answer-key", fixture.teacher()).andExpect(status().isOk());
        assertTrue(contents.isCompleted(fixture.quizId(), fixture.student().getUserId()));
    }

    @ParameterizedTest
    @CsvSource({"false,false,TEXT", "false,true,TEXT", "true,false,TEXT", "true,true,TEXT",
        "false,false,GIT_URL", "false,true,GIT_URL", "true,false,GIT_URL", "true,true,GIT_URL"})
    void personalAndGroupTextAndGitSubmissionsRequireVisibility(boolean coding, boolean group, String type) throws Exception {
        Fixture fixture = fixture();
        Long id = coding ? fixture.codingId() : fixture.assignmentId();
        setFrom(fixture, id, FUTURE);
        submit(fixture, id, type, group).andExpect(status().isForbidden());
        assertTrue(submissions.findMySubmissionHistory(id, fixture.student().getUserId()).isEmpty());
        setFrom(fixture, id, PAST);
        submit(fixture, id, type, group).andExpect(status().isCreated());
        setFrom(fixture, id, FUTURE);
        submit(fixture, id, type, group).andExpect(status().isForbidden());
        getAs("/api/contents/" + id + "/submissions/me", fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].late").value(false));
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "false,true,false", "true,false,false", "true,true,false",
        "false,false,true", "false,true,true", "true,false,true", "true,true,true"})
    void visibilityChangesPreserveReleasedGradesAndHistoricalGroupOwnership(boolean coding, boolean group, boolean closeEnd) throws Exception {
        Fixture fixture = fixture();
        Long id = coding ? fixture.codingId() : fixture.assignmentId();
        var saved = submissions.submit(id, fixture.student().getUserId(), SubmissionType.TEXT, "Answer", group ? fixture.groupId() : null);
        var draft = grades.save(saved.getId(), fixture.teacher().getUserId(), BigDecimal.ZERO, "Passed", null, true);
        var released = grades.release(saved.getId(), fixture.teacher().getUserId(), draft.version());
        if (group) tx(() -> {
            entities.find(Group.class, fixture.groupId()).removeGroupMember(entities.find(User.class, fixture.member().getUserId()));
            return null;
        });
        hide(fixture, id, closeEnd);
        for (User owner : group ? List.of(fixture.student(), fixture.member()) : List.of(fixture.student())) {
            assertTrue(contents.isCompleted(id, owner.getUserId()));
            getAs("/api/submissions/" + saved.getId(), owner).andExpect(status().isOk()).andExpect(jsonPath("$.late").value(false));
            getAs("/api/submissions/" + saved.getId() + "/grade", owner).andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));
        }
        assertEquals(released, grades.review(saved.getId(), fixture.teacher().getUserId()));
        getAs("/api/submissions/" + saved.getId() + "/review", fixture.teacher()).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void hiddenFileSubmissionIsRejectedBeforeStorage(boolean group, boolean closeEnd) throws Exception {
        Fixture fixture = fixture();
        hide(fixture, fixture.assignmentId(), closeEnd);
        file(fixture, group).andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void fileFinalAcceptanceRechecksVisibilityChangedDuringUpload(boolean group, boolean closeEnd) throws Exception {
        Fixture fixture = fixture();
        doAnswer(call -> {
            if (closeEnd) contents.updateVisibilityWindow(fixture.assignmentId(), fixture.teacher().getUserId(), null, PAST);
            else contents.updateVisibility(fixture.assignmentId(), fixture.teacher().getUserId(), FUTURE);
            return new SubmissionObjectStorage.ObjectReference("test", call.getArgument(0));
        }).when(storage).put(any(), any(), anyLong(), any());
        file(fixture, group).andExpect(status().isForbidden());
        assertTrue(submissions.findMySubmissionHistory(fixture.assignmentId(), fixture.student().getUserId()).isEmpty());
        verify(storage).delete(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fileSubmissionIsAcceptedAtStartBoundary(boolean group) throws Exception {
        Fixture fixture = fixture();
        setFrom(fixture, fixture.assignmentId(), NOW);
        file(fixture, group).andExpect(status().isCreated()).andExpect(jsonPath("$.late").value(false));
        verify(storage).put(any(), any(), anyLong(), any());
    }
}
