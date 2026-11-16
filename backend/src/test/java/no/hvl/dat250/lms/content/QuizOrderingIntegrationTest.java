package no.hvl.dat250.lms.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.QuizAnswerRepository;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.content.service.QuizService;
import no.hvl.dat250.lms.content.service.QuizService.ItemDraft;
import no.hvl.dat250.lms.content.service.QuizService.OrderingDraft;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Test-signed JWTs, real filters and SQL; no live Keycloak or developer database. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz_ordering_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizOrderingIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;
    @Autowired private QuizService quizzes;
    @Autowired private ContentService contents;
    @Autowired private QuizAttemptRepository attempts;
    @Autowired private QuizAnswerRepository answers;
    @Autowired private PlatformTransactionManager transactions;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(User teacher, User student, Enrollment enrollment, Module module, Quiz quiz) {
    }

    private Fixture fixture() {
        Course course = save(new Course("DAT250", "Ordering", Term.AUTUMN, 2026));
        User teacher = user();
        User student = user();
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        Enrollment enrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week", 0));
        return new Fixture(teacher, student, enrollment, module, save(new Quiz(module, "Quiz", 0, 3)));
    }

    private OrderingDraft definition() {
        return new OrderingDraft(List.of(new ItemDraft("a", "First item"), new ItemDraft("b", "Second item"),
            new ItemDraft("c", "Third item")), List.of("b", "c", "a"));
    }

    private QuizQuestion ordering(Fixture fixture, int position) {
        return quizzes.addQuestion(fixture.quiz().getId(), "Put in order", position, "Review the sequence",
            List.of(), QuestionType.ORDERING, List.of(), definition());
    }

    private String body(String ordering) {
        return "{\"questionText\":\"Put in order\",\"position\":0,\"type\":\"ORDERING\",\"ordering\":" + ordering + "}";
    }

    private String validDefinition() {
        return """
            {"items":[{"key":"a","text":"First item"},{"key":"b","text":"Second item"},
              {"key":"c","text":"Third item"}],"correctOrder":["b","c","a"]}
            """;
    }

    private MockHttpServletRequestBuilder add(Fixture fixture, User actor, String body) {
        return post("/api/quizzes/{id}/questions", fixture.quiz().getId())
            .header("Authorization", TestTokens.bearer(actor)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private List<UUID> correctIds(QuizQuestion question) {
        return question.getCorrectOrder().stream().map(QuizItem::getId).toList();
    }

    private String idsJson(List<UUID> ids) {
        return ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private String answer(QuizQuestion question, List<UUID> ids) {
        return "{\"questionId\":" + question.getId() + ",\"orderedItemIds\":" + idsJson(ids) + "}";
    }

    private MockHttpServletRequestBuilder submit(Fixture fixture, String answer) {
        return post("/api/quizzes/{id}/attempts", fixture.quiz().getId())
            .header("Authorization", TestTokens.bearer(fixture.student())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"answers\":[" + answer + "]}");
    }

    @Test
    void teacherCreatesOrderingWithRandomIdsAndPersistentCorrectSequence() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(add(fixture, fixture.teacher(), body(validDefinition())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("ORDERING"));
        entities.flush();
        entities.clear();
        QuizQuestion question = quizzes.findQuizWithQuestions(fixture.quiz().getId()).questions().getFirst().question();
        assertEquals(List.of("Second item", "Third item", "First item"),
            question.getCorrectOrder().stream().map(QuizItem::getText).toList());
        assertEquals(3, question.getItems().size());
        assertTrue(question.getItems().stream().allMatch(item -> item.getId().version() == 4));
        assertEquals(fixture.quiz().getId(), question.getQuiz().getId());
    }

    @Test
    void studentReadsStableUuidOrderWithoutCorrectPositionOrTeacherKeys() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = ordering(fixture, 0);
        List<String> displayIds = question.getItems().stream().map(item -> item.getId().toString()).sorted().toList();
        for (int read = 0; read < 2; read++) {
            mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                    .header("Authorization", TestTokens.bearer(fixture.student())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].items", hasSize(3)))
                .andExpect(jsonPath("$.questions[0].items[0].id").value(displayIds.get(0)))
                .andExpect(jsonPath("$.questions[0].items[1].id").value(displayIds.get(1)))
                .andExpect(jsonPath("$.questions[0].items[2].id").value(displayIds.get(2)))
                .andExpect(jsonPath("$.questions[0].items[*].position").isEmpty())
                .andExpect(jsonPath("$.questions[0].items[*].key").isEmpty())
                .andExpect(jsonPath("$.questions[0].correctOrder").doesNotExist())
                .andExpect(jsonPath("$.questions[0].hint").doesNotExist());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void detailAndSubmissionWorkAcrossCommittedTransactionsWithOsivDisabled() throws Exception {
        record Prepared(Fixture fixture, String answer) { }
        Prepared prepared = new TransactionTemplate(transactions).execute(status -> {
            Fixture fixture = fixture();
            QuizQuestion question = ordering(fixture, 0);
            return new Prepared(fixture, answer(question, correctIds(question)));
        });
        Fixture fixture = prepared.fixture();
        mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                .header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].items", hasSize(3)));
        mvc.perform(submit(fixture, prepared.answer())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(1));
    }

    @Test
    void wrongPermutationPersistsInSubmittedOrderAndReturnsHint() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = ordering(fixture, 0);
        List<UUID> wrong = new ArrayList<>(correctIds(question));
        Collections.reverse(wrong);
        mvc.perform(submit(fixture, answer(question, wrong))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(0)).andExpect(jsonPath("$.hints", hasSize(1)))
            .andExpect(jsonPath("$.hints[0].hint").value("Review the sequence"))
            .andExpect(jsonPath("$.correctOrder").doesNotExist());
        entities.flush();
        entities.clear();
        QuizAttempt attempt = attempts.findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(
            fixture.enrollment().getId(), fixture.quiz().getId()).getFirst();
        QuizAnswer stored = answers.findByAttempt_IdOrderByQuestion_PositionAsc(attempt.getId()).getFirst();
        assertEquals(wrong, stored.getOrderedItems().stream().map(QuizItem::getId).toList());
        assertEquals(fixture.enrollment().getId(), stored.getAttempt().getEnrollment().getId());
        assertFalse(stored.isCorrect());
        assertFalse(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
        assertNotEquals(wrong, correctIds(stored.getQuestion()));
    }

    @Test
    void fourQuestionTypesShareScoringPassingAndCompletion() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion ordering = ordering(fixture, 0);
        QuizQuestion mc = quizzes.addQuestion(fixture.quiz().getId(), "MC", 1, null,
            List.of(new OptionDraft("A", 0, true), new OptionDraft("B", 1, false)));
        QuizQuestion tf = quizzes.addQuestion(fixture.quiz().getId(), "TF", 2, null,
            List.of(new OptionDraft("True", 0, true), new OptionDraft("False", 1, false)),
            QuestionType.TRUE_FALSE, List.of());
        QuizQuestion fill = quizzes.addQuestion(fixture.quiz().getId(), "Text", 3, null,
            List.of(), QuestionType.FILL_IN_BLANK, List.of("Java"));
        Long trueOption = entities.createQuery("select o.id from ChoiceOption o where o.question.id = :id and o.correct = true", Long.class)
            .setParameter("id", tf.getId()).getSingleResult();
        String allAnswers = answer(ordering, correctIds(ordering))
            + ",{\"questionId\":" + mc.getId() + ",\"selectedOptionIds\":[]}"
            + ",{\"questionId\":" + tf.getId() + ",\"selectedOptionIds\":[" + trueOption + "]}"
            + ",{\"questionId\":" + fill.getId() + ",\"textAnswer\":\" JAVA \"}";
        mvc.perform(submit(fixture, allAnswers)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(3)).andExpect(jsonPath("$.totalQuestions").value(4))
            .andExpect(jsonPath("$.hints", hasSize(0)));
        assertTrue(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
        fixture.quiz().setPassingPercentage(76);
        assertFalse(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
    }

    @Test
    void incompleteDuplicateForeignAndMixedAnswersAreRejected() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = ordering(fixture, 0);
        Fixture other = fixture();
        QuizQuestion foreign = ordering(other, 0);
        List<UUID> correct = correctIds(question);
        for (String fields : List.of(
                "", ",\"orderedItemIds\":null", ",\"orderedItemIds\":[]",
                ",\"orderedItemIds\":" + idsJson(correct.subList(0, 2)),
                ",\"orderedItemIds\":" + idsJson(List.of(correct.get(0), correct.get(0), correct.get(2))),
                ",\"orderedItemIds\":" + idsJson(List.of(correct.get(0), correct.get(1), correctIds(foreign).get(0))),
                ",\"orderedItemIds\":[null,null,null]", ",\"orderedItemIds\":[\"not-a-uuid\"]",
                ",\"orderedItemIds\":" + idsJson(correct) + ",\"textAnswer\":\"text\"",
                ",\"orderedItemIds\":" + idsJson(correct) + ",\"selectedOptionIds\":[1]")) {
            mvc.perform(submit(fixture, "{\"questionId\":" + question.getId() + fields + "}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0, attempts.countByEnrollment_IdAndQuiz_Id(fixture.enrollment().getId(), fixture.quiz().getId()));
        assertEquals(0L, entities.createQuery("select count(a) from QuizAnswer a where a.question.id = :id", Long.class)
            .setParameter("id", question.getId()).getSingleResult());
    }

    @Test
    void invalidDefinitionsDoNotCreateQuestionsOrItems() throws Exception {
        Fixture fixture = fixture();
        long questionsBefore = entities.createQuery("select count(q) from QuizQuestion q", Long.class).getSingleResult();
        long itemsBefore = entities.createQuery("select count(i) from QuizItem i", Long.class).getSingleResult();
        for (String definition : List.of("null", "{}",
                "{\"items\":[],\"correctOrder\":[]}",
                "{\"items\":[{\"key\":\"a\",\"text\":\"A\"}],\"correctOrder\":[\"a\"]}",
                "{\"items\":[{\"key\":\"a\",\"text\":\"A\"},{\"key\":\"a\",\"text\":\"B\"}],\"correctOrder\":[\"a\",\"a\"]}",
                "{\"items\":[{\"key\":\"a\",\"text\":\"A\"},{\"key\":\"b\",\"text\":\" A \"}],\"correctOrder\":[\"a\",\"b\"]}",
                validDefinition().replace("[\"b\",\"c\",\"a\"]", "[\"b\",\"b\",\"a\"]"),
                validDefinition().replace("[\"b\",\"c\",\"a\"]", "[\"b\",\"c\",\"unknown\"]"))) {
            mvc.perform(add(fixture, fixture.teacher(), body(definition))).andExpect(status().isBadRequest());
        }
        assertEquals(questionsBefore, entities.createQuery("select count(q) from QuizQuestion q", Long.class).getSingleResult());
        assertEquals(itemsBefore, entities.createQuery("select count(i) from QuizItem i", Long.class).getSingleResult());
    }

    @Test
    void orderingCannotBeMixedIntoOtherQuestionOrAnswerTypes() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(add(fixture, fixture.teacher(), body(validDefinition()).replace("\"ORDERING\"", "\"FILL_IN_BLANK\"")))
            .andExpect(status().isBadRequest());
        mvc.perform(add(fixture, fixture.teacher(), body(validDefinition()).replace("\"ordering\":", "\"acceptedAnswers\":[\"A\"],\"ordering\":")))
            .andExpect(status().isBadRequest());
        QuizQuestion fill = quizzes.addQuestion(fixture.quiz().getId(), "Text", 0, null,
            List.of(), QuestionType.FILL_IN_BLANK, List.of("Java"));
        mvc.perform(submit(fixture, "{\"questionId\":" + fill.getId() + ",\"textAnswer\":\"Java\","
                + "\"orderedItemIds\":[\"" + UUID.randomUUID() + "\"]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void existingPermissionsAndQuestionFreezeApplyToOrdering() throws Exception {
        Fixture fixture = fixture();
        Fixture outsider = fixture();
        for (User denied : new User[]{fixture.student(), outsider.teacher()}) {
            mvc.perform(add(fixture, denied, body(validDefinition()))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/quizzes/{id}/questions", fixture.quiz().getId())
                .contentType(MediaType.APPLICATION_JSON).content(body(validDefinition())))
            .andExpect(status().isUnauthorized());
        QuizQuestion question = ordering(fixture, 0);
        mvc.perform(submit(fixture, answer(question, correctIds(question)))).andExpect(status().isCreated());
        mvc.perform(add(fixture, fixture.teacher(), body(validDefinition()))).andExpect(status().isConflict());
    }
}
