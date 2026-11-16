package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.QuizAnswerRepository;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.content.service.QuizService;
import no.hvl.dat250.lms.content.service.QuizService.OptionDraft;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real security filters and SQL, with test-signed JWTs rather than live Keycloak. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz_types_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizTypesIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;
    @Autowired private QuizService quizzes;
    @Autowired private QuizAttemptRepository attempts;
    @Autowired private QuizAnswerRepository answers;
    @Autowired private ContentService contents;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(User teacher, User student, Enrollment studentEnrollment, Module module, Quiz quiz) {
    }

    private Fixture fixture() {
        Course course = save(new Course("DAT250", "Quiz types", Term.AUTUMN, 2026));
        User teacher = user();
        User student = user();
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        Enrollment enrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week", 0));
        return new Fixture(teacher, student, enrollment, module, save(new Quiz(module, "Quiz", 0, 3)));
    }

    private QuizQuestion choice(Fixture fixture, QuestionType type, int position, String hint) {
        return quizzes.addQuestion(fixture.quiz().getId(), "Choose", position, hint,
            List.of(new OptionDraft("True", 0, true), new OptionDraft("False", 1, false)), type, List.of());
    }

    private QuizQuestion fill(Fixture fixture, int position, String hint) {
        return quizzes.addQuestion(fixture.quiz().getId(), "Language?", position, hint,
            List.of(), QuestionType.FILL_IN_BLANK, List.of("Java", "Java language"));
    }

    private List<ChoiceOption> options(QuizQuestion question) {
        return entities.createQuery("select o from ChoiceOption o where o.question.id = :id order by o.position", ChoiceOption.class)
            .setParameter("id", question.getId()).getResultList();
    }

    private MockHttpServletRequestBuilder submit(Fixture fixture, String body) {
        return post("/api/quizzes/{id}/attempts", fixture.quiz().getId())
            .header("Authorization", TestTokens.bearer(fixture.student()))
            .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String questionBody(String fields) {
        return "{\"questionText\":\"Question\",\"position\":0," + fields + "}";
    }

    private MockHttpServletRequestBuilder add(Fixture fixture, User actor, String body) {
        return post("/api/quizzes/{id}/questions", fixture.quiz().getId())
            .header("Authorization", TestTokens.bearer(actor))
            .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void mixedAttemptPersistsRawTextAndUsesExistingPassingRules() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion mc = choice(fixture, QuestionType.MULTIPLE_CHOICE, 0, "Read the choices");
        QuizQuestion tf = choice(fixture, QuestionType.TRUE_FALSE, 1, "Think again");
        QuizQuestion fill = fill(fixture, 2, "Consider JVM languages");
        String body = """
            {"answers":[
              {"questionId":%d,"selectedOptionIds":[%d]},
              {"questionId":%d,"selectedOptionIds":[%d]},
              {"questionId":%d,"textAnswer":"  jAvA  "}
            ]}
            """.formatted(mc.getId(), options(mc).get(0).getId(), tf.getId(), options(tf).get(1).getId(), fill.getId());
        mvc.perform(submit(fixture, body)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(2)).andExpect(jsonPath("$.totalQuestions").value(3))
            .andExpect(jsonPath("$.hints", hasSize(1)))
            .andExpect(jsonPath("$.hints[0].questionId").value(tf.getId().intValue()))
            .andExpect(jsonPath("$.hints[0].hint").value("Think again"))
            .andExpect(jsonPath("$.acceptedAnswers").doesNotExist())
            .andExpect(jsonPath("$.hints[0].correctOptionIds").doesNotExist());
        entities.flush();
        entities.clear();
        QuizAttempt attempt = attempts.findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(
            fixture.studentEnrollment().getId(), fixture.quiz().getId()).getFirst();
        List<QuizAnswer> stored = answers.findByAttempt_IdOrderByQuestion_PositionAsc(attempt.getId());
        assertEquals(3, stored.size());
        assertEquals("  jAvA  ", stored.get(2).getTextAnswer());
        assertTrue(stored.get(2).isCorrect());
        assertTrue(stored.get(2).getSelectedOptions().isEmpty());
        assertTrue(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
        entities.find(Quiz.class, fixture.quiz().getId()).setPassingPercentage(67);
        assertFalse(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
    }

    @Test
    void hintOnlyAppearsForWrongAnswersWithNonblankConfiguredHints() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion hinted = fill(fixture, 0, "Think about the JVM");
        QuizQuestion noHint = fill(fixture, 1, null);
        QuizQuestion blankHint = fill(fixture, 2, "   ");
        String body = """
            {"answers":[{"questionId":%d,"textAnswer":"wrong"},
              {"questionId":%d,"textAnswer":""},{"questionId":%d,"textAnswer":"   "}]}
            """.formatted(hinted.getId(), noHint.getId(), blankHint.getId());
        mvc.perform(submit(fixture, body)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(0)).andExpect(jsonPath("$.hints", hasSize(1)))
            .andExpect(jsonPath("$.hints[0].hint").value("Think about the JVM"));
        String correct = """
            {"answers":[{"questionId":%d,"textAnswer":"JAVA LANGUAGE"},
              {"questionId":%d,"textAnswer":"java"},{"questionId":%d,"textAnswer":"Java"}]}
            """.formatted(hinted.getId(), noHint.getId(), blankHint.getId());
        mvc.perform(submit(fixture, correct)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(3)).andExpect(jsonPath("$.hints", hasSize(0)));
    }

    @Test
    void legacyMultipleChoiceWrongAnswerReturnsOnlyItsHint() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = choice(fixture, QuestionType.MULTIPLE_CHOICE, 0, "Review the material");
        mvc.perform(submit(fixture, "{\"answers\":[{\"questionId\":" + question.getId()
                + ",\"selectedOptionIds\":[]}]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.correctCount").value(0))
            .andExpect(jsonPath("$.hints", hasSize(1)))
            .andExpect(jsonPath("$.hints[0].hint").value("Review the material"));
    }

    @Test
    void studentAndTeacherDetailsExposeTypesButNeverKeysOrHints() throws Exception {
        Fixture fixture = fixture();
        choice(fixture, QuestionType.TRUE_FALSE, 0, "Private hint");
        fill(fixture, 1, "Another hint");
        for (User actor : new User[]{fixture.student(), fixture.teacher()}) {
            mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                    .header("Authorization", TestTokens.bearer(actor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].type").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.questions[1].type").value("FILL_IN_BLANK"))
                .andExpect(jsonPath("$.questions[1].options", hasSize(0)))
                .andExpect(jsonPath("$.questions[*].hint").isEmpty())
                .andExpect(jsonPath("$.questions[*].acceptedAnswers").isEmpty())
                .andExpect(jsonPath("$.questions[*].options[*].correct").isEmpty());
        }
    }

    @Test
    void teacherCanCreateTypedQuestionsAndLegacyRequestDefaultsToMultipleChoice() throws Exception {
        Fixture fixture = fixture();
        String choices = "\"options\":[{\"optionText\":\"True\",\"position\":0,\"correct\":true},"
            + "{\"optionText\":\"False\",\"position\":1,\"correct\":false}]";
        mvc.perform(add(fixture, fixture.teacher(), questionBody(choices)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"));
        mvc.perform(add(fixture, fixture.teacher(), questionBody("\"type\":\"TRUE_FALSE\"," + choices)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("TRUE_FALSE"));
        mvc.perform(add(fixture, fixture.teacher(), questionBody(
                "\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"Java\",\"Java language\"]")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("FILL_IN_BLANK"));
        entities.flush();
        entities.clear();
        var details = quizzes.findQuizWithQuestions(fixture.quiz().getId());
        assertEquals(3, details.questions().size());
        assertTrue(details.questions().stream().anyMatch(item -> item.question().getAcceptedAnswers().contains("Java")));
    }

    @Test
    void invalidQuestionDefinitionsAreRejectedWithoutSaving() throws Exception {
        Fixture fixture = fixture();
        for (String fields : List.of(
                "\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[]",
                "\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"   \"]",
                "\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[null]",
                "\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"" + "a".repeat(256) + "\"]",
                "\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"Java\"],\"options\":["
                    + "{\"optionText\":\"A\",\"position\":0,\"correct\":true},{\"optionText\":\"B\",\"position\":1,\"correct\":false}]",
                "\"type\":\"TRUE_FALSE\",\"options\":["
                    + "{\"optionText\":\"A\",\"position\":0,\"correct\":true},{\"optionText\":\"B\",\"position\":1,\"correct\":true}]",
                "\"options\":[{\"optionText\":\"A\",\"position\":0,\"correct\":true},"
                    + "{\"optionText\":\"B\",\"position\":1,\"correct\":false}],\"acceptedAnswers\":[\"Java\"]",
                "\"type\":\"ORDERING\"")) {
            mvc.perform(add(fixture, fixture.teacher(), questionBody(fields))).andExpect(status().isBadRequest());
        }
        assertEquals(0L, entities.createQuery("select count(q) from QuizQuestion q", Long.class).getSingleResult());
    }

    @Test
    void malformedAnswersDoNotPersistAttemptsOrConsumeLimit() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion fill = fill(fixture, 0, "Hint");
        for (String fields : List.of(
                "", ",\"textAnswer\":null", ",\"selectedOptionIds\":[123],\"textAnswer\":\"Java\"",
                ",\"textAnswer\":\"" + "a".repeat(256) + "\"")) {
            mvc.perform(submit(fixture, "{\"answers\":[{\"questionId\":" + fill.getId() + fields + "}]}"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(submit(fixture, "{\"answers\":[null]}")).andExpect(status().isBadRequest());
        assertEquals(0, attempts.count());
        assertEquals(0, answers.count());
    }

    @Test
    void trueFalseRejectsEmptyMultipleForeignOptionsAndText() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = choice(fixture, QuestionType.TRUE_FALSE, 0, "Hint");
        List<ChoiceOption> choices = options(question);
        for (String fields : List.of("\"selectedOptionIds\":[]", "\"selectedOptionIds\":[999999]",
                "\"selectedOptionIds\":[" + choices.get(0).getId() + "," + choices.get(1).getId() + "]",
                "\"selectedOptionIds\":[" + choices.get(0).getId() + "],\"textAnswer\":\"True\"")) {
            mvc.perform(submit(fixture, "{\"answers\":[{\"questionId\":" + question.getId() + "," + fields + "}]}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0, attempts.count());
    }

    @Test
    void questionsCannotBeAddedAfterAnAttempt() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = fill(fixture, 0, null);
        mvc.perform(submit(fixture, "{\"answers\":[{\"questionId\":" + question.getId() + ",\"textAnswer\":\"Java\"}]}"))
            .andExpect(status().isCreated());
        mvc.perform(add(fixture, fixture.teacher(), questionBody("\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"Java\"]")))
            .andExpect(status().isConflict());
    }

    @Test
    void quizAuthoringAndReadingRequireActiveCourseRoles() throws Exception {
        Fixture fixture = fixture();
        Fixture outsider = fixture();
        User inactive = user();
        Enrollment inactiveEnrollment = save(new Enrollment(inactive, fixture.module().getCourse(), CourseRole.TEACHER));
        inactiveEnrollment.deactivate();
        entities.flush();
        String createPath = "/api/courses/" + fixture.module().getCourse().getId()
            + "/modules/" + fixture.module().getId() + "/contents/quizzes";
        String createBody = "{\"title\":\"New quiz\",\"position\":1,\"maxAttempts\":2}";
        for (User denied : new User[]{fixture.student(), outsider.teacher(), inactive}) {
            mvc.perform(add(fixture, denied, questionBody("\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"Java\"]")))
                .andExpect(status().isForbidden());
            mvc.perform(post(createPath).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(createBody)).andExpect(status().isForbidden());
        }
        for (User denied : new User[]{outsider.student(), outsider.teacher(), inactive}) {
            mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                    .header("Authorization", TestTokens.bearer(denied))).andExpect(status().isForbidden());
        }
        mvc.perform(post(createPath).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(createBody)).andExpect(status().isCreated());
        mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())).andExpect(status().isUnauthorized());
        mvc.perform(post(createPath).contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/quizzes/{id}/questions", fixture.quiz().getId())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownQuizReturns404ForReadAndAuthoring() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(get("/api/quizzes/99999999").header("Authorization", TestTokens.bearer(fixture.teacher())))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/quizzes/99999999/questions").header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(questionBody("\"type\":\"FILL_IN_BLANK\",\"acceptedAnswers\":[\"Java\"]")))
            .andExpect(status().isNotFound());
    }
}
