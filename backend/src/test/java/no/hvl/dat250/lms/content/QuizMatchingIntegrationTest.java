package no.hvl.dat250.lms.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
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
import no.hvl.dat250.lms.content.service.QuizService.MatchingDraft;
import no.hvl.dat250.lms.content.service.QuizService.MatchDraft;
import no.hvl.dat250.lms.content.service.QuizGrader.MatchIds;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Test-signed JWTs, real filters and SQL; no live Keycloak or developer database. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz_matching_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizMatchingIntegrationTest {
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

    private MatchingDraft definition() {
        return new MatchingDraft(
            List.of(new ItemDraft("l1", "Java"), new ItemDraft("l2", "React")),
            List.of(new ItemDraft("r1", "UI library"), new ItemDraft("r2", "Language")),
            List.of(new MatchDraft("l1", "r2"), new MatchDraft("l2", "r1")));
    }

    private QuizQuestion matching(Fixture fixture, int position) {
        return quizzes.addQuestion(fixture.quiz().getId(), "Match the terms", position, "Review the concepts",
            List.of(), QuestionType.MATCHING, List.of(), null, definition());
    }

    private String validDefinition() {
        return """
            {"leftItems":[{"key":"l1","text":"Java"},{"key":"l2","text":"React"}],
             "rightItems":[{"key":"r1","text":"UI library"},{"key":"r2","text":"Language"}],
             "correctMatches":[{"leftKey":"l1","rightKey":"r2"},{"leftKey":"l2","rightKey":"r1"}]}
            """;
    }

    private String body(String matching) {
        return "{\"questionText\":\"Match\",\"position\":0,\"type\":\"MATCHING\",\"matching\":" + matching + "}";
    }

    private MockHttpServletRequestBuilder add(Fixture fixture, User actor, String body) {
        return post("/api/quizzes/{id}/questions", fixture.quiz().getId())
            .header("Authorization", TestTokens.bearer(actor)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private List<MatchIds> correctPairs(QuizQuestion question) {
        return question.getCorrectMatches().stream()
            .map(pair -> new MatchIds(pair.getLeftItem().getId(), pair.getRightItem().getId())).toList();
    }

    private String pairsJson(List<MatchIds> pairs) {
        return pairs.stream().map(pair -> "{\"leftItemId\":\"" + pair.leftItemId()
            + "\",\"rightItemId\":\"" + pair.rightItemId() + "\"}")
            .collect(Collectors.joining(",", "[", "]"));
    }

    private String answer(QuizQuestion question, List<MatchIds> pairs) {
        return "{\"questionId\":" + question.getId() + ",\"matches\":" + pairsJson(pairs) + "}";
    }

    private MockHttpServletRequestBuilder submit(Fixture fixture, String answer) {
        return post("/api/quizzes/{id}/attempts", fixture.quiz().getId())
            .header("Authorization", TestTokens.bearer(fixture.student())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"answers\":[" + answer + "]}");
    }

    @Test
    void teacherCreatesDistinctSidesAndPersistsCorrectPairs() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(add(fixture, fixture.teacher(), body(validDefinition())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("MATCHING"));
        entities.flush();
        entities.clear();
        QuizQuestion question = quizzes.findQuizWithQuestions(fixture.quiz().getId()).questions().getFirst().question();
        assertEquals(4, question.getItems().size());
        assertTrue(question.getItems().stream().allMatch(item -> item.getId().version() == 4));
        assertEquals(2, question.getItems().stream().filter(item -> item.getKind() == QuizItemKind.MATCHING_LEFT).count());
        assertEquals(2, question.getItems().stream().filter(item -> item.getKind() == QuizItemKind.MATCHING_RIGHT).count());
        var texts = question.getCorrectMatches().stream().collect(Collectors.toMap(
            pair -> pair.getLeftItem().getText(), pair -> pair.getRightItem().getText()));
        assertEquals(Map.of("Java", "Language", "React", "UI library"), texts);
    }

    @Test
    void studentReadsIndependentUuidSortedSidesWithoutPairingMetadata() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = matching(fixture, 0);
        List<String> leftIds = question.getItems().stream().filter(item -> item.getKind() == QuizItemKind.MATCHING_LEFT)
            .map(item -> item.getId().toString()).sorted().toList();
        List<String> rightIds = question.getItems().stream().filter(item -> item.getKind() == QuizItemKind.MATCHING_RIGHT)
            .map(item -> item.getId().toString()).sorted().toList();
        for (int read = 0; read < 2; read++) {
            mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                    .header("Authorization", TestTokens.bearer(fixture.student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].leftItems", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].rightItems", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].leftItems[0].id").value(leftIds.get(0)))
                .andExpect(jsonPath("$.questions[0].leftItems[1].id").value(leftIds.get(1)))
                .andExpect(jsonPath("$.questions[0].rightItems[0].id").value(rightIds.get(0)))
                .andExpect(jsonPath("$.questions[0].rightItems[1].id").value(rightIds.get(1)))
                .andExpect(jsonPath("$.questions[0].items", hasSize(0)))
                .andExpect(jsonPath("$.questions[0].correctMatches").doesNotExist())
                .andExpect(content().string(not(containsString("\"correctMatches\""))))
                .andExpect(content().string(not(containsString("\"leftKey\""))))
                .andExpect(content().string(not(containsString("\"rightKey\""))))
                .andExpect(content().string(not(containsString("\"leftItemId\""))))
                .andExpect(content().string(not(containsString("\"rightItemId\""))))
                .andExpect(jsonPath("$.questions[0].leftItems[*].key").isEmpty())
                .andExpect(jsonPath("$.questions[0].rightItems[*].position").isEmpty())
                .andExpect(jsonPath("$.questions[0].leftItems[*].rightItemId").isEmpty())
                .andExpect(jsonPath("$.questions[0].hint").doesNotExist());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void matchingReadAndSubmissionWorkWithoutAnOuterTransactionOrOsiv() throws Exception {
        record Prepared(Fixture fixture, String answer) { }
        Prepared prepared = new TransactionTemplate(transactions).execute(status -> {
            Fixture fixture = fixture();
            QuizQuestion question = matching(fixture, 0);
            return new Prepared(fixture, answer(question, correctPairs(question)));
        });
        Fixture fixture = prepared.fixture();
        mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                .header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].leftItems", hasSize(2)));
        mvc.perform(submit(fixture, prepared.answer())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(1)).andExpect(jsonPath("$.hints", hasSize(0)));
    }

    @Test
    void wrongCompletePairsPersistAndReturnHintWhilePairOrderDoesNotAffectCorrectness() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = matching(fixture, 0);
        List<MatchIds> correct = correctPairs(question);
        List<MatchIds> wrong = List.of(new MatchIds(correct.get(0).leftItemId(), correct.get(1).rightItemId()),
            new MatchIds(correct.get(1).leftItemId(), correct.get(0).rightItemId()));
        mvc.perform(submit(fixture, answer(question, wrong))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(0)).andExpect(jsonPath("$.hints", hasSize(1)))
            .andExpect(jsonPath("$.hints[0].hint").value("Review the concepts"))
            .andExpect(jsonPath("$.correctMatches").doesNotExist());
        entities.flush();
        entities.clear();
        QuizAttempt attempt = attempts.findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(
            fixture.enrollment().getId(), fixture.quiz().getId()).getFirst();
        QuizAnswer stored = answers.findByAttempt_IdOrderByQuestion_PositionAsc(attempt.getId()).getFirst();
        assertEquals(new HashSet<>(wrong), stored.getMatches().stream()
            .map(pair -> new MatchIds(pair.getLeftItem().getId(), pair.getRightItem().getId()))
            .collect(Collectors.toSet()));
        assertFalse(stored.isCorrect());
        assertEquals(fixture.enrollment().getId(), stored.getAttempt().getEnrollment().getId());
        assertFalse(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
        List<MatchIds> reordered = new ArrayList<>(correct);
        Collections.reverse(reordered);
        mvc.perform(submit(fixture, answer(question, reordered))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(1)).andExpect(jsonPath("$.hints", hasSize(0)));
        assertTrue(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
    }

    @Test
    void incompleteDuplicateWrongSideAndForeignPairsAreRejected() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion question = matching(fixture, 0);
        List<MatchIds> pairs = correctPairs(question);
        QuizQuestion foreign = matching(fixture(), 0);
        List<MatchIds> foreignPairs = correctPairs(foreign);
        List<String> fields = List.of(
            "", ",\"matches\":null", ",\"matches\":[]", ",\"matches\":[null]",
            ",\"matches\":[{\"leftItemId\":null,\"rightItemId\":null}]",
            ",\"matches\":[{\"leftItemId\":\"invalid\",\"rightItemId\":\"invalid\"}]",
            ",\"matches\":" + pairsJson(pairs.subList(0, 1)),
            ",\"matches\":" + pairsJson(List.of(pairs.get(0), pairs.get(0))),
            ",\"matches\":" + pairsJson(List.of(pairs.get(0),
                new MatchIds(pairs.get(1).leftItemId(), pairs.get(0).rightItemId()))),
            ",\"matches\":" + pairsJson(List.of(pairs.get(0),
                new MatchIds(pairs.get(0).leftItemId(), pairs.get(1).rightItemId()))),
            ",\"matches\":" + pairsJson(List.of(pairs.get(0), foreignPairs.get(1))),
            ",\"matches\":" + pairsJson(List.of(pairs.get(0),
                new MatchIds(pairs.get(1).rightItemId(), pairs.get(1).leftItemId()))),
            ",\"matches\":" + pairsJson(pairs) + ",\"textAnswer\":\"text\"",
            ",\"matches\":" + pairsJson(pairs) + ",\"selectedOptionIds\":[1]",
            ",\"matches\":" + pairsJson(pairs) + ",\"orderedItemIds\":[\"" + UUID.randomUUID() + "\"]");
        for (String extra : fields) {
            mvc.perform(submit(fixture, "{\"questionId\":" + question.getId() + extra + "}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0, attempts.countByEnrollment_IdAndQuiz_Id(fixture.enrollment().getId(), fixture.quiz().getId()));
        assertEquals(0L, entities.createQuery("select count(a) from QuizAnswer a where a.question.id = :id", Long.class)
            .setParameter("id", question.getId()).getSingleResult());
    }

    @Test
    void invalidDefinitionsAndMixedFieldsNeverCreateMatchingData() throws Exception {
        Fixture fixture = fixture();
        long before = entities.createQuery("select count(q) from QuizQuestion q", Long.class).getSingleResult();
        for (String definition : List.of("null", "{}",
                validDefinition().replace("\"key\":\"l2\"", "\"key\":\"l1\""),
                validDefinition().replace("\"text\":\"Language\"", "\"text\":\" UI library \""),
                validDefinition().replace("\"rightKey\":\"r1\"", "\"rightKey\":\"r2\""),
                validDefinition().replace("\"leftKey\":\"l1\"", "\"leftKey\":\"unknown\""),
                validDefinition().replace("{\"key\":\"r2\",\"text\":\"Language\"}", "null"),
                validDefinition().replace("{\"leftKey\":\"l2\",\"rightKey\":\"r1\"}", "null"))) {
            mvc.perform(add(fixture, fixture.teacher(), body(definition))).andExpect(status().isBadRequest());
        }
        for (String modified : List.of(
                body(validDefinition()).replace("\"MATCHING\"", "\"FILL_IN_BLANK\""),
                body(validDefinition()).replace("\"matching\":", "\"ordering\":{\"items\":[],\"correctOrder\":[]},\"matching\":"),
                body(validDefinition()).replace("\"matching\":", "\"acceptedAnswers\":[\"answer\"],\"matching\":"))) {
            mvc.perform(add(fixture, fixture.teacher(), modified)).andExpect(status().isBadRequest());
        }
        assertEquals(before, entities.createQuery("select count(q) from QuizQuestion q", Long.class).getSingleResult());
        assertEquals(0L, entities.createQuery("select count(i) from QuizItem i where i.question.quiz.id = :id", Long.class)
            .setParameter("id", fixture.quiz().getId()).getSingleResult());
    }

    @Test
    void allFiveTypesReusePassingPercentageCompletionAndOrderingPersistence() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion match = matching(fixture, 0);
        QuizQuestion order = quizzes.addQuestion(fixture.quiz().getId(), "Order", 1, null,
            List.of(), QuestionType.ORDERING, List.of(),
            new OrderingDraft(List.of(new ItemDraft("a", "A"), new ItemDraft("b", "B")), List.of("b", "a")));
        QuizQuestion mc = quizzes.addQuestion(fixture.quiz().getId(), "MC", 2, null,
            List.of(new OptionDraft("A", 0, true), new OptionDraft("B", 1, false)));
        QuizQuestion tf = quizzes.addQuestion(fixture.quiz().getId(), "TF", 3, null,
            List.of(new OptionDraft("True", 0, true), new OptionDraft("False", 1, false)), QuestionType.TRUE_FALSE, List.of());
        QuizQuestion fill = quizzes.addQuestion(fixture.quiz().getId(), "Text", 4, null,
            List.of(), QuestionType.FILL_IN_BLANK, List.of("Java"));
        Long trueOption = entities.createQuery("select o.id from ChoiceOption o where o.question.id = :id and o.correct = true", Long.class)
            .setParameter("id", tf.getId()).getSingleResult();
        String orderIds = order.getCorrectOrder().stream().map(item -> "\"" + item.getId() + "\"")
            .collect(Collectors.joining(",", "[", "]"));
        String allAnswers = answer(match, correctPairs(match))
            + ",{\"questionId\":" + order.getId() + ",\"orderedItemIds\":" + orderIds + "}"
            + ",{\"questionId\":" + mc.getId() + ",\"selectedOptionIds\":[]}"
            + ",{\"questionId\":" + tf.getId() + ",\"selectedOptionIds\":[" + trueOption + "]}"
            + ",{\"questionId\":" + fill.getId() + ",\"textAnswer\":\" JAVA \"}";
        mvc.perform(submit(fixture, allAnswers)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.correctCount").value(4)).andExpect(jsonPath("$.totalQuestions").value(5));
        assertTrue(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
        fixture.quiz().setPassingPercentage(81);
        assertFalse(contents.isCompleted(fixture.quiz().getId(), fixture.student().getUserId()));
        entities.flush();
        entities.clear();
        QuizAttempt attempt = attempts.findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(
            fixture.enrollment().getId(), fixture.quiz().getId()).getFirst();
        QuizAnswer storedOrder = answers.findByAttempt_IdOrderByQuestion_PositionAsc(attempt.getId()).get(1);
        assertEquals(order.getCorrectOrder().stream().map(QuizItem::getId).toList(),
            storedOrder.getOrderedItems().stream().map(QuizItem::getId).toList());
        assertTrue(storedOrder.getOrderedItems().stream().allMatch(item -> item.getKind() == QuizItemKind.ORDERING));
    }

    @Test
    void matchingRespectsExistingPermissionsAndFirstAttemptFreeze() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        User inactive = user();
        Enrollment teacher = save(new Enrollment(inactive, fixture.module().getCourse(), CourseRole.TEACHER));
        teacher.deactivate();
        entities.flush();
        for (User denied : new User[]{fixture.student(), other.teacher(), inactive}) {
            mvc.perform(add(fixture, denied, body(validDefinition()))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/quizzes/{id}/questions", fixture.quiz().getId())
                .contentType(MediaType.APPLICATION_JSON).content(body(validDefinition())))
            .andExpect(status().isUnauthorized());
        QuizQuestion question = matching(fixture, 0);
        mvc.perform(submit(fixture, answer(question, correctPairs(question)))).andExpect(status().isCreated());
        mvc.perform(add(fixture, fixture.teacher(), body(validDefinition()))).andExpect(status().isConflict());
        mvc.perform(get("/api/quizzes/{id}", fixture.quiz().getId())
                .header("Authorization", TestTokens.bearer(other.student()))).andExpect(status().isForbidden());
    }

    @Test
    void otherAnswerTypesRejectPairsAndMatchingAttemptsRemainPrivate() throws Exception {
        Fixture fixture = fixture();
        QuizQuestion fill = quizzes.addQuestion(fixture.quiz().getId(), "Text", 0, null,
            List.of(), QuestionType.FILL_IN_BLANK, List.of("Java"));
        String pairs = "[{\"leftItemId\":\"" + UUID.randomUUID() + "\",\"rightItemId\":\"" + UUID.randomUUID() + "\"}]";
        mvc.perform(submit(fixture, "{\"questionId\":" + fill.getId() + ",\"textAnswer\":\"Java\",\"matches\":" + pairs + "}"))
            .andExpect(status().isBadRequest());
        Fixture matchFixture = fixture();
        QuizQuestion question = matching(matchFixture, 0);
        mvc.perform(post("/api/quizzes/{id}/attempts", matchFixture.quiz().getId())
                .header("Authorization", TestTokens.bearer(fixture.student()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[" + answer(question, correctPairs(question)) + "]}"))
            .andExpect(status().isForbidden());
        assertEquals(0, attempts.countByEnrollment_IdAndQuiz_Id(matchFixture.enrollment().getId(), matchFixture.quiz().getId()));
    }
}
