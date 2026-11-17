package no.hvl.dat250.lms.content;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.service.*;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed data, real JWT/course checks and no request-spanning transaction (OSIV disabled). */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:content_editing_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentEditingIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private ContentService contents;
    @Autowired private AssignmentService assignments;
    @Autowired private CodingExerciseService codingExercises;
    @Autowired private QuizService quizzes;

    private static final Instant DEADLINE = Instant.parse("2030-01-01T12:00:00Z");
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
    private record Fixture(Long courseId, Long moduleId, User teacher, User student,
                           Long teacherEnrollmentId, Long studentEnrollmentId, Map<String, Long> ids) {
    }
    private Fixture fixture() {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Editing", Term.AUTUMN, 2026));
            User teacher = user(), student = user();
            Enrollment teacherEnrollment = save(new Enrollment(teacher, course, CourseRole.TEACHER));
            Enrollment studentEnrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            StaticPage page = save(new StaticPage(module, "Original", 2, "Original body"));
            Assignment assignment = save(new Assignment(module, "Original", 6, "Original instructions", DEADLINE, true));
            CodingExercise coding = save(new CodingExercise(module, "Original", 9, "Original instructions",
                "https://example.com/old", "old code", DEADLINE, true));
            Quiz quiz = save(new Quiz(module, "Original", 11, 5));
            assignment.setMandatoryForExam(true);
            quiz.setPassingPercentage(75);
            save(new QuizQuestion(quiz, "Original question", 0, "Hint", QuestionType.FILL_IN_BLANK, List.of("secret")));
            for (Content content : List.of(page, assignment, coding, quiz)) {
                content.setRequiredForModuleCompletion(true);
            }
            return new Fixture(course.getId(), module.getId(), teacher, student,
                teacherEnrollment.getId(), studentEnrollment.getId(),
                Map.of("page", page.getId(), "assignment", assignment.getId(), "coding", coding.getId(), "quiz", quiz.getId()));
        });
    }
    private String path(String type, Long id) {
        return switch (type) {
            case "page" -> "/api/contents/" + id;
            case "assignment" -> "/api/assignments/" + id;
            case "coding" -> "/api/coding-exercises/" + id;
            case "quiz" -> "/api/quizzes/" + id;
            default -> throw new IllegalArgumentException(type);
        };
    }
    private String body(String type) {
        return switch (type) {
            case "page" -> "{\"title\":\"Updated\",\"body\":\"Updated body\"}";
            case "assignment" -> "{\"title\":\"Updated\",\"instructions\":\"Updated instructions\"}";
            case "coding" -> """
                {"title":"Updated","instructions":"Updated instructions",
                 "starterRepositoryUrl":"https://example.com/new","starterCode":"  // TODO\\n"}
                """;
            case "quiz" -> "{\"title\":\"Updated\"}";
            default -> throw new IllegalArgumentException(type);
        };
    }
    private ResultActions update(Fixture fixture, String type, String body) throws Exception {
        return mvc.perform(put(path(type, fixture.ids().get(type)))
            .header("Authorization", TestTokens.bearer(fixture.teacher()))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private void assertProtectedFields(Fixture fixture) {
        tx(() -> {
            for (String type : List.of("page", "assignment", "coding", "quiz")) {
                Content content = entities.find(Content.class, fixture.ids().get(type));
                assertEquals(fixture.moduleId(), content.getModule().getId());
                assertTrue(content.isRequiredForModuleCompletion());
                assertEquals(switch (type) { case "page" -> 2; case "assignment" -> 6; case "coding" -> 9; default -> 11; },
                    content.getPosition());
            }
            Assignment assignment = entities.find(Assignment.class, fixture.ids().get("assignment"));
            CodingExercise coding = entities.find(CodingExercise.class, fixture.ids().get("coding"));
            Quiz quiz = entities.find(Quiz.class, fixture.ids().get("quiz"));
            assertEquals(DEADLINE, assignment.getDeadline());
            assertEquals(DEADLINE, coding.getDeadline());
            assertTrue(assignment.isAllowLateSubmission());
            assertTrue(coding.isAllowLateSubmission());
            assertTrue(assignment.isMandatoryForExam());
            assertEquals(5, quiz.getMaxAttempts());
            assertEquals(75, quiz.getPassingPercentage());
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "assignment", "coding", "quiz"})
    void teacherUpdatesAndStudentReadsPersistedFields(String type) throws Exception {
        Fixture fixture = fixture();
        update(fixture, type, body(type)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(fixture.ids().get(type)))
            .andExpect(jsonPath("$.title").value("Updated"));
        var read = mvc.perform(get(path(type, fixture.ids().get(type)))
                .header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
        switch (type) {
            case "page" -> read.andExpect(jsonPath("$.body").value("Updated body"));
            case "assignment" -> read.andExpect(jsonPath("$.instructions").value("Updated instructions"));
            case "coding" -> read.andExpect(jsonPath("$.instructions").value("Updated instructions"))
                .andExpect(jsonPath("$.starterRepositoryUrl").value("https://example.com/new"))
                .andExpect(jsonPath("$.starterCode").value("  // TODO\n"));
            case "quiz" -> read.andExpect(jsonPath("$.maxAttempts").value(5))
                .andExpect(jsonPath("$.passingPercentage").value(75));
        }
        assertProtectedFields(fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "assignment", "coding", "quiz"})
    void editRequiresActiveCourseTeacher(String type) throws Exception {
        Fixture fixture = fixture(), other = fixture();
        User inactive = tx(() -> {
            User teacher = user();
            Enrollment enrollment = save(new Enrollment(teacher, entities.find(Course.class, fixture.courseId()), CourseRole.TEACHER));
            enrollment.deactivate();
            return teacher;
        });
        String path = path(type, fixture.ids().get(type));
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body(type)))
            .andExpect(status().isUnauthorized());
        for (User denied : List.of(fixture.student(), other.teacher(), inactive)) {
            mvc.perform(put(path).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(body(type)))
                .andExpect(status().isForbidden());
        }
        assertEquals("Original", tx(() -> entities.find(Content.class, fixture.ids().get(type)).getTitle()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "assignment", "coding", "quiz"})
    void missingAndWrongTypeIdsReturn404(String type) throws Exception {
        Fixture fixture = fixture();
        Long wrongTypeId = fixture.ids().get(type.equals("page") ? "quiz" : "page");
        for (Long id : List.of(Long.MAX_VALUE, wrongTypeId)) {
            mvc.perform(put(path(type, id)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(body(type)))
                .andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "assignment", "coding", "quiz"})
    void invalidTitleRejectsEntireUpdate(String type) throws Exception {
        Fixture fixture = fixture();
        for (String invalid : List.of("null", "\"   \"", "\"" + "x".repeat(256) + "\"")) {
            update(fixture, type, body(type).replace("\"Updated\"", invalid)).andExpect(status().isBadRequest());
        }
        update(fixture, type, body(type).replace("\"title\":\"Updated\",", "")
            .replace("\"title\":\"Updated\"", "")).andExpect(status().isBadRequest());
        tx(() -> {
            assertEquals("Original", entities.find(Content.class, fixture.ids().get(type)).getTitle());
            if (type.equals("coding")) {
                assertEquals("old code", entities.find(CodingExercise.class, fixture.ids().get(type)).getStarterCode());
            }
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "assignment", "coding"})
    void invalidRequiredTextRejectsEntireUpdate(String type) throws Exception {
        Fixture fixture = fixture();
        String field = type.equals("page") ? "body" : "instructions";
        String value = type.equals("page") ? "Updated body" : "Updated instructions";
        for (String invalid : List.of("null", "\" \"", "\"" + "x".repeat(10001) + "\"")) {
            update(fixture, type, body(type).replace("\"" + value + "\"", invalid))
                .andExpect(status().isBadRequest());
        }
        update(fixture, type, body(type).replace("\"" + field + "\":\"" + value + "\",", "")
            .replace(",\"" + field + "\":\"" + value + "\"", ""))
            .andExpect(status().isBadRequest());
        assertEquals("Original", tx(() -> entities.find(Content.class, fixture.ids().get(type)).getTitle()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "omitted", "empty"})
    void starterCodeCanBeCleared(String value) throws Exception {
        Fixture fixture = fixture();
        String code = switch (value) { case "null" -> ",\"starterCode\":null"; case "empty" -> ",\"starterCode\":\"\""; default -> ""; };
        update(fixture, "coding", "{\"title\":\"Updated\",\"instructions\":\"Work\",\"starterRepositoryUrl\":\"https://example.com/new\"" + code + "}")
            .andExpect(status().isOk()).andExpect(value.equals("empty")
                ? jsonPath("$.starterCode").value("") : jsonPath("$.starterCode").isEmpty());
        assertEquals(value.equals("empty") ? "" : null,
            tx(() -> entities.find(CodingExercise.class, fixture.ids().get("coding")).getStarterCode()));
    }

    @Test
    void codingFieldLimitsAreValidated() throws Exception {
        Fixture fixture = fixture();
        for (String invalid : List.of("null", "\" \"", "\"" + "x".repeat(2049) + "\"")) {
            update(fixture, "coding", body("coding").replace("\"https://example.com/new\"", invalid))
                .andExpect(status().isBadRequest());
        }
        update(fixture, "coding", "{\"title\":\"Updated\",\"instructions\":\"Work\",\"starterCode\":null}")
            .andExpect(status().isBadRequest());
        update(fixture, "coding", body("coding").replace("  // TODO\\n", "x".repeat(10001)))
            .andExpect(status().isBadRequest());
        update(fixture, "coding", "{\"title\":\"" + "t".repeat(255) + "\",\"instructions\":\"" + "i".repeat(10000)
            + "\",\"starterRepositoryUrl\":\"" + "r".repeat(2048) + "\",\"starterCode\":\"" + "c".repeat(10000) + "\"}")
            .andExpect(status().isOk());
        assertProtectedFields(fixture);
    }

    @Test
    void editingAfterParticipationPreservesHistoryCompletionAndQuizKeys() throws Exception {
        Fixture fixture = fixture();
        List<Long> submissions = tx(() -> {
            var owner = entities.find(Enrollment.class, fixture.studentEnrollmentId());
            var teacher = entities.find(Enrollment.class, fixture.teacherEnrollmentId());
            var page = entities.find(StaticPage.class, fixture.ids().get("page"));
            save(new StaticPageCompletion(page, owner, Instant.now()));
            Quiz quiz = entities.find(Quiz.class, fixture.ids().get("quiz"));
            save(new QuizAttempt(quiz, owner, Instant.now(), 1, 1));
            List<Long> ids = new java.util.ArrayList<>();
            for (String type : List.of("assignment", "coding")) {
                Content content = entities.find(Content.class, fixture.ids().get(type));
                var submission = save(new Submission(content, owner, SubmissionType.TEXT, "Original answer", Instant.now(), false));
                var grade = new Grade(submission, BigDecimal.ZERO, "Original feedback", teacher, Instant.now());
                grade.decidePassed(true); grade.release(teacher, Instant.now()); save(grade);
                ids.add(submission.getId());
            }
            return ids;
        });
        // Title editing remains allowed after attempts; question authoring is still frozen.
        for (String type : List.of("page", "assignment", "coding", "quiz")) {
            update(fixture, type, body(type)).andExpect(status().isOk());
            assertTrue(contents.isCompleted(fixture.ids().get(type), fixture.student().getUserId()));
        }
        for (Long id : submissions) {
            mvc.perform(get("/api/submissions/{id}", id).header("Authorization", TestTokens.bearer(fixture.student())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payload").value("Original answer"));
            mvc.perform(get("/api/submissions/{id}/grade", id).header("Authorization", TestTokens.bearer(fixture.student())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.feedback").value("Original feedback"));
        }
        mvc.perform(post("/api/quizzes/{id}/questions", fixture.ids().get("quiz"))
                .header("Authorization", TestTokens.bearer(fixture.teacher())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionText\":\"New\",\"position\":0,\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"answer\"]}"))
            .andExpect(status().isConflict());
        tx(() -> {
            QuizQuestion question = entities.createQuery(
                    "select q from QuizQuestion q where q.quiz.id = :quizId", QuizQuestion.class)
                .setParameter("quizId", fixture.ids().get("quiz")).getSingleResult();
            assertEquals("Original question", question.getQuestionText());
            assertEquals(java.util.Set.of("secret"), question.getAcceptedAnswers());
            assertEquals(1L, entities.createQuery(
                    "select count(a) from QuizAttempt a where a.quiz.id = :quizId", Long.class)
                .setParameter("quizId", fixture.ids().get("quiz")).getSingleResult());
            return null;
        });
        assertProtectedFields(fixture);
    }

    @Test
    void internalServiceCallsAlsoRejectInvalidDetailsWithoutPartialWrites() {
        Fixture fixture = fixture();
        assertThrows(IllegalArgumentException.class,
            () -> contents.updateStaticPage(fixture.ids().get("page"), "Changed", " "));
        assertThrows(IllegalArgumentException.class,
            () -> assignments.updateDetails(fixture.ids().get("assignment"), "Changed", "x".repeat(10001)));
        assertThrows(IllegalArgumentException.class,
            () -> codingExercises.updateDetails(fixture.ids().get("coding"), "Changed", "Work", " ", null));
        assertThrows(IllegalArgumentException.class,
            () -> quizzes.updateTitle(fixture.ids().get("quiz"), "x".repeat(256)));
        for (Long id : fixture.ids().values()) {
            assertEquals("Original", tx(() -> entities.find(Content.class, id).getTitle()));
        }
    }
}
