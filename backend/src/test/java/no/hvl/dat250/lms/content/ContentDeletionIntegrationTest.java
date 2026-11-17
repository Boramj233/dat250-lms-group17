package no.hvl.dat250.lms.content;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import no.hvl.dat250.lms.content.repository.StaticPageCompletionRepository;
import no.hvl.dat250.lms.content.service.*;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.grade.service.GradingService;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed data, real JWT filters, actual H2 constraints and no live object storage. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:content_deletion_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentDeletionIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ContentService contents;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ContentDeletionService deletion;
    @Autowired private ContentCompletionService completion;
    @Autowired private QuizService quizzes;
    @Autowired private QuizAttemptRepository attempts;
    @Autowired private StaticPageCompletionRepository pageCompletions;
    @Autowired private SubmissionService submissions;
    @Autowired private GradingService grades;
    @MockitoBean private SubmissionObjectStorage storage;

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
                           Long quizId, Long questionId, Long correctId, Long wrongId,
                           Long studentEnrollmentId, Long groupId, User teacher, User student, User member) {
        List<Long> contentIds() { return List.of(pageId, assignmentId, codingId, quizId); }
    }
    private Fixture fixture() {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Deletion", Term.AUTUMN, 2026));
            User teacher = user(), student = user(), member = user();
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            Enrollment studentEnrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
            save(new Enrollment(member, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            StaticPage page = save(new StaticPage(module, "Page", 0, "Body"));
            Assignment assignment = save(new Assignment(module, "Assignment", 1, "Instructions", null, false));
            assignment.setMandatoryForExam(true);
            CodingExercise coding = save(new CodingExercise(module, "Coding", 2, "Instructions", "https://example.com/repo", null, false));
            Quiz quiz = save(new Quiz(module, "Quiz", 3, 3));
            QuizQuestion question = save(new QuizQuestion(quiz, "Choose", 0, "Hint"));
            ChoiceOption correct = save(new ChoiceOption(question, "Correct", 0, true));
            ChoiceOption wrong = save(new ChoiceOption(question, "Wrong", 1, false));
            for (Content content : List.of(page, assignment, coding, quiz)) content.setRequiredForModuleCompletion(true);
            Group group = new Group(1, course);
            group.addGroupMember(student);
            group.addGroupMember(member);
            save(group);
            return new Fixture(course.getId(), module.getId(), page.getId(), assignment.getId(), coding.getId(),
                quiz.getId(), question.getId(), correct.getId(), wrong.getId(), studentEnrollment.getId(),
                group.getGroupId(), teacher, student, member);
        });
    }
    private String path(Long id) { return "/api/contents/" + id; }
    private String list(Fixture fixture) {
        return "/api/courses/" + fixture.courseId() + "/modules/" + fixture.moduleId() + "/contents";
    }
    private ResultActions remove(Long id, User teacher) throws Exception {
        return mvc.perform(delete(path(id)).header("Authorization", TestTokens.bearer(teacher)));
    }
    private ResultActions getAs(String path, User user) throws Exception {
        return mvc.perform(get(path).header("Authorization", TestTokens.bearer(user)));
    }
    private void submitQuiz(Fixture fixture, boolean correct) throws Exception {
        mvc.perform(post("/api/quizzes/{id}/attempts", fixture.quizId())
                .header("Authorization", TestTokens.bearer(fixture.student())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":" + fixture.questionId() + ",\"selectedOptionIds\":["
                    + (correct ? fixture.correctId() : fixture.wrongId()) + "]}]}"))
            .andExpect(status().isCreated());
    }
    private List<Long> addOtherQuestionTypes(Fixture fixture) {
        return tx(() -> {
            quizzes.addQuestion(fixture.quizId(), "True or false", 1, null,
                List.of(new QuizService.OptionDraft("True", 0, true), new QuizService.OptionDraft("False", 1, false)),
                QuestionType.TRUE_FALSE, List.of());
            quizzes.addQuestion(fixture.quizId(), "Fill blank", 2, "Hint", List.of(), QuestionType.FILL_IN_BLANK,
                List.of("Java", "Kotlin"));
            quizzes.addQuestion(fixture.quizId(), "Order", 3, null, List.of(), QuestionType.ORDERING, List.of(),
                new QuizService.OrderingDraft(List.of(new QuizService.ItemDraft("a", "First"),
                    new QuizService.ItemDraft("b", "Second")), List.of("b", "a")));
            quizzes.addQuestion(fixture.quizId(), "Match", 4, null, List.of(), QuestionType.MATCHING, List.of(), null,
                new QuizService.MatchingDraft(
                    List.of(new QuizService.ItemDraft("l1", "Left one"), new QuizService.ItemDraft("l2", "Left two")),
                    List.of(new QuizService.ItemDraft("r1", "Right one"), new QuizService.ItemDraft("r2", "Right two")),
                    List.of(new QuizService.MatchDraft("l1", "r2"), new QuizService.MatchDraft("l2", "r1"))));
            return quizzes.findQuizWithQuestions(fixture.quizId()).questions().stream().map(item -> item.question().getId()).toList();
        });
    }
    private Map<String, Long> definitionCounts(List<Long> questionIds) {
        // Only fixed table names and server-generated fixture IDs enter this test SQL.
        String ids = questionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : List.of("quiz_questions", "choice_options", "quiz_items", "quiz_accepted_answers",
                "quiz_question_correct_order", "quiz_question_correct_matches")) {
            String column = table.equals("quiz_questions") ? "id" : "question_id";
            counts.put(table, jdbc.queryForObject("select count(*) from " + table + " where " + column + " in (" + ids + ")", Long.class));
        }
        return counts;
    }

    @Test
    void teacherCanDeleteAllUnusedTypesIncludingHiddenRequiredAndMandatoryContent() throws Exception {
        Fixture fixture = fixture();
        for (Long id : fixture.contentIds()) {
            contents.updateVisibilityWindow(id, fixture.teacher().getUserId(), null, Instant.now().minusSeconds(60));
            remove(id, fixture.teacher()).andExpect(status().isNoContent()).andExpect(content().string(""));
            assertFalse(contentRepository.existsById(id));
            assertThrows(NoSuchElementException.class, () -> contents.requireContent(id));
            remove(id, fixture.teacher()).andExpect(status().isNotFound());
        }
        getAs(list(fixture), fixture.teacher()).andExpect(status().isOk()).andExpect(content().json("[]"));
        assertNotNull(tx(() -> entities.find(Module.class, fixture.moduleId())));
        assertNotNull(tx(() -> entities.find(Course.class, fixture.courseId())));
        verifyNoInteractions(storage);
    }

    @Test
    void deleteFreesPositionWithoutRenumberingOrChangingOtherContent() throws Exception {
        Fixture fixture = fixture();
        remove(fixture.assignmentId(), fixture.teacher()).andExpect(status().isNoContent());
        getAs(list(fixture), fixture.teacher()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].position").value(0)).andExpect(jsonPath("$[1].position").value(2))
            .andExpect(jsonPath("$[2].position").value(3));
        mvc.perform(post(list(fixture) + "/assignments").header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Replacement\",\"position\":1,\"instructions\":\"New\",\"allowLateSubmission\":false}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.position").value(1));
        getAs("/api/assignments/" + fixture.assignmentId(), fixture.teacher()).andExpect(status().isNotFound());
    }

    @Test
    void deletingQuizCleansAllFiveQuestionTypesAndLeavesOtherQuizDefinitionsUntouched() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        List<Long> targetIds = addOtherQuestionTypes(fixture);
        List<Long> otherIds = addOtherQuestionTypes(other);
        Map<String, Long> before = definitionCounts(targetIds);
        assertEquals(Map.of("quiz_questions", 5L, "choice_options", 4L, "quiz_items", 6L,
            "quiz_accepted_answers", 2L, "quiz_question_correct_order", 2L, "quiz_question_correct_matches", 2L), before);
        Map<String, Long> otherBefore = definitionCounts(otherIds);
        remove(fixture.quizId(), fixture.teacher()).andExpect(status().isNoContent());
        definitionCounts(targetIds).forEach((table, count) -> assertEquals(0L, count, table));
        assertEquals(otherBefore, definitionCounts(otherIds));
        assertFalse(contentRepository.existsById(fixture.quizId()));
        getAs("/api/quizzes/" + fixture.quizId(), fixture.teacher()).andExpect(status().isNotFound());
        getAs("/api/quizzes/" + other.quizId() + "/answer-key", other.teacher()).andExpect(status().isOk())
            .andExpect(jsonPath("$.questions.length()").value(5));
    }

    @Test
    void emptyQuizCanAlsoBeDeleted() throws Exception {
        Fixture fixture = fixture();
        Long quizId = tx(() -> save(new Quiz(entities.find(Module.class, fixture.moduleId()), "Empty", 4, 1)).getId());
        remove(quizId, fixture.teacher()).andExpect(status().isNoContent());
        assertFalse(contentRepository.existsById(quizId));
    }

    @Test
    void authenticationAndActualCourseTeacherAreRequiredForEveryType() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        for (Long id : fixture.contentIds()) {
            mvc.perform(delete(path(id))).andExpect(status().isUnauthorized());
            mvc.perform(delete(path(id)).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
            for (User denied : List.of(fixture.student(), fixture.member(), other.teacher(), other.student())) {
                mvc.perform(delete(path(id)).header("Authorization", TestTokens.bearer(denied))
                        .param("teacherUserId", fixture.teacher().getUserId().toString()))
                    .andExpect(status().isForbidden());
            }
            assertTrue(contentRepository.existsById(id));
        }
        assertThrows(AccessDeniedException.class, () -> deletion.delete(fixture.pageId(), fixture.student().getUserId()));
        tx(() -> {
            entities.createQuery("select e from Enrollment e where e.user.userId=:user and e.course.id=:course", Enrollment.class)
                .setParameter("user", fixture.teacher().getUserId()).setParameter("course", fixture.courseId())
                .getSingleResult().deactivate();
            return null;
        });
        for (Long id : fixture.contentIds()) remove(id, fixture.teacher()).andExpect(status().isForbidden());
        remove(Long.MAX_VALUE, other.teacher()).andExpect(status().isNotFound());
        remove(0L, other.teacher()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"false,false,NONE", "false,true,NONE", "true,false,NONE", "true,true,NONE",
        "false,false,DRAFT", "false,true,DRAFT", "true,false,DRAFT", "true,true,DRAFT",
        "false,false,FAIL", "false,true,FAIL", "true,false,FAIL", "true,true,FAIL",
        "false,false,PASS", "false,true,PASS", "true,false,PASS", "true,true,PASS"})
    void anyPersonalOrGroupSubmissionBlocksDeletionRegardlessOfGrade(boolean coding, boolean group, String gradeState) throws Exception {
        Fixture fixture = fixture();
        Long id = coding ? fixture.codingId() : fixture.assignmentId();
        var saved = submissions.submit(id, fixture.student().getUserId(), coding ? SubmissionType.GIT_URL : SubmissionType.TEXT,
            "https://example.com/answer", group ? fixture.groupId() : null);
        if (!gradeState.equals("NONE")) {
            var draft = grades.save(saved.getId(), fixture.teacher().getUserId(), BigDecimal.TEN, "Feedback", null,
                gradeState.equals("DRAFT") ? null : gradeState.equals("PASS"));
            if (!gradeState.equals("DRAFT")) grades.release(saved.getId(), fixture.teacher().getUserId(), draft.version());
        }
        if (group) tx(() -> {
            entities.find(Group.class, fixture.groupId()).removeGroupMember(entities.find(User.class, fixture.member().getUserId()));
            return null;
        });
        var gradeBefore = gradeState.equals("NONE") ? null : grades.review(saved.getId(), fixture.teacher().getUserId());
        mvc.perform(delete(path(id)).header("Authorization", TestTokens.bearer(fixture.teacher())).param("force", "true"))
            .andExpect(status().isConflict());
        assertTrue(contentRepository.existsById(id));
        assertTrue(submissions.hasSubmissions(id));
        for (User owner : group ? List.of(fixture.student(), fixture.member()) : List.of(fixture.student())) {
            getAs("/api/submissions/" + saved.getId(), owner).andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("https://example.com/answer")).andExpect(jsonPath("$.late").value(false));
            assertEquals(gradeState.equals("PASS"), contents.isCompleted(id, owner.getUserId()));
            if (gradeState.equals("PASS") || gradeState.equals("FAIL")) {
                getAs("/api/submissions/" + saved.getId() + "/grade", owner).andExpect(status().isOk())
                    .andExpect(jsonPath("$.passed").value(gradeState.equals("PASS")));
            }
        }
        if (gradeBefore != null) assertEquals(gradeBefore, grades.review(saved.getId(), fixture.teacher().getUserId()));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fileSubmissionMetadataAndObjectsAreNeverRemoved(boolean group) throws Exception {
        Fixture fixture = fixture();
        SubmissionFileMetadata file = new SubmissionFileMetadata("test", "submissions/" + UUID.randomUUID(), "report.pdf",
            "application/pdf", 10, "a".repeat(64));
        var saved = submissions.saveFileSubmission(fixture.assignmentId(), fixture.student().getUserId(), file,
            group ? fixture.groupId() : null);
        remove(fixture.assignmentId(), fixture.teacher()).andExpect(status().isConflict());
        var retained = submissions.findOwnSubmission(saved.getId(), fixture.student().getUserId()).getFile();
        assertEquals(file.getObjectKey(), retained.getObjectKey());
        assertEquals(file.getSha256(), retained.getSha256());
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedAndPassingQuizAttemptsBothBlockDeletionAndRetainAnswersHintsAndKeys(boolean passed) throws Exception {
        Fixture fixture = fixture();
        submitQuiz(fixture, passed);
        Long attemptId = attempts.findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(fixture.studentEnrollmentId(), fixture.quizId()).getFirst().getId();
        String keyPath = "/api/quizzes/" + fixture.quizId() + "/answer-key";
        String keyBefore = getAs(keyPath, fixture.teacher()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        remove(fixture.quizId(), fixture.teacher()).andExpect(status().isConflict());
        assertEquals(1, attempts.countByEnrollment_IdAndQuiz_Id(fixture.studentEnrollmentId(), fixture.quizId()));
        assertEquals(passed, contents.isCompleted(fixture.quizId(), fixture.student().getUserId()));
        var result = getAs("/api/quizzes/" + fixture.quizId() + "/attempts/" + attemptId, fixture.student())
            .andExpect(status().isOk()).andExpect(jsonPath("$.answers[0].correct").value(passed));
        if (passed) result.andExpect(jsonPath("$.hints").isEmpty());
        else result.andExpect(jsonPath("$.hints[0].hint").value("Hint"));
        getAs(keyPath, fixture.teacher()).andExpect(status().isOk()).andExpect(content().json(keyBefore));
    }

    @Test
    void pageCompletionBlocksDeletionEvenWhenPageIsHidden() throws Exception {
        Fixture fixture = fixture();
        completion.markPage(fixture.pageId(), fixture.student().getUserId(), true);
        contents.updateVisibilityWindow(fixture.pageId(), fixture.teacher().getUserId(), null, Instant.now().minusSeconds(60));
        remove(fixture.pageId(), fixture.teacher()).andExpect(status().isConflict());
        assertTrue(pageCompletions.existsByContent_Id(fixture.pageId()));
        assertTrue(contents.isCompleted(fixture.pageId(), fixture.student().getUserId()));
        contents.updateVisibilityWindow(fixture.pageId(), fixture.teacher().getUserId(), null, null);
        getAs(path(fixture.pageId()) + "/completion", fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void withdrawalDoesNotMakeHistoricalContentDeletable() throws Exception {
        Fixture fixture = fixture();
        submissions.submitIndividual(fixture.assignmentId(), fixture.student().getUserId(), SubmissionType.TEXT, "Answer");
        submitQuiz(fixture, false);
        completion.markPage(fixture.pageId(), fixture.student().getUserId(), true);
        tx(() -> {
            entities.find(Enrollment.class, fixture.studentEnrollmentId()).deactivate();
            return null;
        });
        for (Long id : List.of(fixture.pageId(), fixture.assignmentId(), fixture.quizId())) {
            remove(id, fixture.teacher()).andExpect(status().isConflict());
            assertTrue(contentRepository.existsById(id));
        }
        assertTrue(submissions.hasSubmissions(fixture.assignmentId()));
        assertTrue(attempts.existsByQuiz_Id(fixture.quizId()));
        assertTrue(pageCompletions.existsByContent_Id(fixture.pageId()));
    }

    @Test
    void unexpectedForeignKeyConflictRollsBackAllQuizDefinitionCleanup() throws Exception {
        Fixture fixture = fixture();
        List<Long> ids = addOtherQuestionTypes(fixture);
        Map<String, Long> before = definitionCounts(ids);
        String keyPath = "/api/quizzes/" + fixture.quizId() + "/answer-key";
        String keyBefore = getAs(keyPath, fixture.teacher()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // A test-only reference simulates another database dependency; never touches the development schema.
        jdbc.execute("create table if not exists deletion_test_references (content_id bigint primary key references contents(id))");
        jdbc.update("insert into deletion_test_references(content_id) values (?)", fixture.quizId());
        remove(fixture.quizId(), fixture.teacher()).andExpect(status().isConflict());
        assertTrue(contentRepository.existsById(fixture.quizId()));
        assertEquals(before, definitionCounts(ids));
        getAs(keyPath, fixture.teacher()).andExpect(status().isOk()).andExpect(content().json(keyBefore));
    }
}
