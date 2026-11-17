package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import no.hvl.dat250.lms.content.repository.QuizAnswerRepository;
import no.hvl.dat250.lms.content.service.QuizService;
import no.hvl.dat250.lms.content.service.QuizGrader.MatchIds;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters and committed five-type submissions; lazy DTO reads run with OSIV disabled. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz_attempt_history_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizAttemptHistoryIntegrationTest {
    private static final String FILL_KEY = "PRIVATE_FILL_KEY";
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private QuizService quizzes;
    @Autowired private QuizAttemptRepository attempts;
    @Autowired private QuizAnswerRepository answers;

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
    private record Fixture(Long quizId, Long studentEnrollmentId, User student, User peer, User teacher,
                           List<Long> questionIds, List<Long> correctChoices, Long wrongChoice,
                           Long trueChoice, Long falseChoice, List<UUID> correctOrder, List<MatchIds> correctMatches) {
    }
    private Fixture fixture() {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Attempt history", Term.AUTUMN, 2026));
            User student = user(), peer = user(), teacher = user();
            Enrollment enrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
            save(new Enrollment(peer, course, CourseRole.STUDENT));
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            Module module = save(new Module(course, "Week", 0));
            Quiz quiz = save(new Quiz(module, "Quiz", 0, 2));
            QuizQuestion mc = quizzes.addQuestion(quiz.getId(), "Select options", 0, "MC hint",
                List.of(new QuizService.OptionDraft("A", 0, true), new QuizService.OptionDraft("B", 1, false),
                    new QuizService.OptionDraft("C", 2, true)), QuestionType.MULTIPLE_CHOICE, List.of());
            var options = entities.createQuery("select o from ChoiceOption o where o.question.id = :id order by o.position",
                ChoiceOption.class).setParameter("id", mc.getId()).getResultList();
            QuizQuestion tf = quizzes.addQuestion(quiz.getId(), "True or false", 1, " ",
                List.of(new QuizService.OptionDraft("True", 0, true), new QuizService.OptionDraft("False", 1, false)),
                QuestionType.TRUE_FALSE, List.of());
            var tfOptions = entities.createQuery("select o from ChoiceOption o where o.question.id = :id order by o.position",
                ChoiceOption.class).setParameter("id", tf.getId()).getResultList();
            QuizQuestion fill = quizzes.addQuestion(quiz.getId(), "Fill the blank", 2, "Fill hint",
                List.of(), QuestionType.FILL_IN_BLANK, List.of(FILL_KEY));
            QuizQuestion ordering = quizzes.addQuestion(quiz.getId(), "Put in order", 3, null,
                List.of(), QuestionType.ORDERING, List.of(), new QuizService.OrderingDraft(
                    List.of(new QuizService.ItemDraft("a", "First"), new QuizService.ItemDraft("b", "Second"),
                        new QuizService.ItemDraft("c", "Third")), List.of("b", "c", "a")));
            QuizQuestion matching = quizzes.addQuestion(quiz.getId(), "Match", 4, "Matching hint",
                List.of(), QuestionType.MATCHING, List.of(), null, new QuizService.MatchingDraft(
                    List.of(new QuizService.ItemDraft("l1", "Left one"), new QuizService.ItemDraft("l2", "Left two")),
                    List.of(new QuizService.ItemDraft("r1", "Right one"), new QuizService.ItemDraft("r2", "Right two")),
                    List.of(new QuizService.MatchDraft("l1", "r2"), new QuizService.MatchDraft("l2", "r1"))));
            return new Fixture(quiz.getId(), enrollment.getId(), student, peer, teacher,
                List.of(mc.getId(), tf.getId(), fill.getId(), ordering.getId(), matching.getId()),
                List.of(options.get(0).getId(), options.get(2).getId()), options.get(1).getId(),
                tfOptions.get(0).getId(), tfOptions.get(1).getId(),
                ordering.getCorrectOrder().stream().map(QuizItem::getId).toList(),
                matching.getCorrectMatches().stream().map(pair -> new MatchIds(pair.getLeftItem().getId(), pair.getRightItem().getId())).toList());
        });
    }
    private List<MatchIds> submittedMatches(Fixture fixture, boolean correct) {
        var pairs = fixture.correctMatches();
        return correct ? pairs : List.of(new MatchIds(pairs.get(0).leftItemId(), pairs.get(1).rightItemId()),
            new MatchIds(pairs.get(1).leftItemId(), pairs.get(0).rightItemId()));
    }
    private String submission(Fixture fixture, boolean correct) {
        var ids = fixture.questionIds();
        List<UUID> order = correct ? fixture.correctOrder() : fixture.correctOrder().reversed();
        String orderedIds = order.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
        String pairs = submittedMatches(fixture, correct).stream().map(pair ->
            "{\"leftItemId\":\"" + pair.leftItemId() + "\",\"rightItemId\":\"" + pair.rightItemId() + "\"}")
            .collect(Collectors.joining(","));
        return "{\"answers\":["
            + "{\"questionId\":" + ids.get(0) + ",\"selectedOptionIds\":" + (correct ? fixture.correctChoices() : List.of(fixture.wrongChoice())) + "},"
            + "{\"questionId\":" + ids.get(1) + ",\"selectedOptionIds\":[" + (correct ? fixture.trueChoice() : fixture.falseChoice()) + "]},"
            + "{\"questionId\":" + ids.get(2) + ",\"textAnswer\":\"" + (correct ? FILL_KEY : "  my answer  ") + "\"},"
            + "{\"questionId\":" + ids.get(3) + ",\"orderedItemIds\":[" + orderedIds + "]},"
            + "{\"questionId\":" + ids.get(4) + ",\"matches\":[" + pairs + "]}]}";
    }
    private Long submit(Fixture fixture, boolean correct) throws Exception {
        mvc.perform(post(base(fixture)).header("Authorization", TestTokens.bearer(fixture.student()))
                .contentType(MediaType.APPLICATION_JSON).content(submission(fixture, correct)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.correctCount").value(correct ? 5 : 0));
        return attempts.findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(fixture.studentEnrollmentId(), fixture.quizId())
            .getFirst().getId();
    }
    private String base(Fixture fixture) {
        return "/api/quizzes/" + fixture.quizId() + "/attempts";
    }
    private ResultActions getAs(String path, User user) throws Exception {
        return mvc.perform(get(path).header("Authorization", TestTokens.bearer(user)));
    }
    private void noAnswerKeys(ResultActions response) throws Exception {
        response.andExpect(content().string(not(containsString("\"acceptedAnswers\""))))
            .andExpect(content().string(not(containsString("\"correctOrder\""))))
            .andExpect(content().string(not(containsString("\"correctMatches\""))))
            .andExpect(content().string(not(containsString("\"correctOptionIds\""))))
            .andExpect(content().string(not(containsString("\"enrollmentId\""))))
            .andExpect(jsonPath("$.answers[*].question.options[*].correct").isEmpty())
            .andExpect(jsonPath("$.answers[*].question.hint").isEmpty());
    }

    @Test
    void newStudentGetsZeroCountAndEmptyHistory() throws Exception {
        Fixture fixture = fixture();
        getAs(base(fixture) + "/me/count", fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.usedAttempts").value(0))
            .andExpect(jsonPath("$.remainingAttempts").doesNotExist())
            .andExpect(header().string("Cache-Control", containsString("no-store")));
        getAs(base(fixture) + "/me", fixture.student()).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void detailsReturnAllFiveSubmittedAnswerTypesAndOnlyWrongQuestionHints() throws Exception {
        Fixture fixture = fixture();
        Long attemptId = submit(fixture, false);
        var detail = getAs(base(fixture) + "/" + attemptId, fixture.student())
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(attemptId))
            .andExpect(jsonPath("$.quizId").value(fixture.quizId()))
            .andExpect(jsonPath("$.correctCount").value(0)).andExpect(jsonPath("$.totalQuestions").value(5))
            .andExpect(jsonPath("$.answers.length()").value(5))
            .andExpect(jsonPath("$.answers[0].questionId").value(fixture.questionIds().get(0)))
            .andExpect(jsonPath("$.answers[0].question.type").value("MULTIPLE_CHOICE"))
            .andExpect(jsonPath("$.answers[0].question.options.length()").value(3))
            .andExpect(jsonPath("$.answers[0].selectedOptionIds[0]").value(fixture.wrongChoice()))
            .andExpect(jsonPath("$.answers[0].correct").value(false))
            .andExpect(jsonPath("$.answers[1].selectedOptionIds[0]").value(fixture.falseChoice()))
            .andExpect(jsonPath("$.answers[1].question.type").value("TRUE_FALSE"))
            .andExpect(jsonPath("$.answers[2].textAnswer").value("  my answer  "))
            .andExpect(jsonPath("$.answers[2].question.type").value("FILL_IN_BLANK"))
            .andExpect(jsonPath("$.answers[3].question.type").value("ORDERING"))
            .andExpect(jsonPath("$.answers[3].orderedItemIds[0]").value(fixture.correctOrder().reversed().getFirst().toString()))
            .andExpect(jsonPath("$.answers[3].question.items.length()").value(3))
            .andExpect(jsonPath("$.answers[4].question.type").value("MATCHING"))
            .andExpect(jsonPath("$.answers[4].question.leftItems.length()").value(2))
            .andExpect(jsonPath("$.answers[4].question.rightItems.length()").value(2))
            .andExpect(jsonPath("$.hints.length()").value(3))
            .andExpect(jsonPath("$.hints[0].hint").value("MC hint"))
            .andExpect(jsonPath("$.hints[1].hint").value("Fill hint"))
            .andExpect(jsonPath("$.hints[2].hint").value("Matching hint"))
            .andExpect(header().string("Cache-Control", containsString("private")))
            .andExpect(content().string(not(containsString(FILL_KEY))));
        List<MatchIds> submitted = submittedMatches(fixture, false).stream()
            .sorted(Comparator.comparing(pair -> pair.leftItemId().toString())).toList();
        for (int index = 0; index < submitted.size(); index++) {
            detail.andExpect(jsonPath("$.answers[4].matches[" + index + "].leftItemId").value(submitted.get(index).leftItemId().toString()))
                .andExpect(jsonPath("$.answers[4].matches[" + index + "].rightItemId").value(submitted.get(index).rightItemId().toString()));
        }
        noAnswerKeys(detail);
        assertEquals(1, attempts.countByEnrollment_IdAndQuiz_Id(fixture.studentEnrollmentId(), fixture.quizId()));
        assertEquals(5, answers.findByAttempt_IdOrderByQuestion_PositionAsc(attemptId).size());
    }

    @Test
    void correctAttemptHasStoredScoreAndNoHints() throws Exception {
        Fixture fixture = fixture();
        Long id = submit(fixture, true);
        var result = getAs(base(fixture) + "/" + id, fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.correctCount").value(5)).andExpect(jsonPath("$.hints").isEmpty());
        for (int index = 0; index < 5; index++) {
            result.andExpect(jsonPath("$.answers[" + index + "].correct").value(true));
        }
        noAnswerKeys(result);
    }

    @Test
    void historyIsNewestFirstWithStableIdTieBreakAndReadsDoNotConsumeAttempts() throws Exception {
        Fixture fixture = fixture();
        Long first = submit(fixture, false), second = submit(fixture, true);
        tx(() -> {
            entities.createQuery("update QuizAttempt a set a.submittedAt=:time where a.quiz.id=:quizId")
                .setParameter("time", Instant.parse("2026-01-01T00:00:00Z")).setParameter("quizId", fixture.quizId()).executeUpdate();
            return null;
        });
        for (int request = 0; request < 2; request++) {
            getAs(base(fixture) + "/me", fixture.student()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].correctCount").value(5)).andExpect(jsonPath("$[1].id").value(first))
                .andExpect(jsonPath("$[1].correctCount").value(0))
                .andExpect(jsonPath("$[0].answers").doesNotExist());
            getAs(base(fixture) + "/me/count", fixture.student()).andExpect(status().isOk())
                .andExpect(jsonPath("$.usedAttempts").value(2));
        }
        var detail = getAs(base(fixture) + "/" + first, fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.correctCount").value(0)).andExpect(jsonPath("$.hints.length()").value(3))
            .andExpect(content().string(not(containsString(FILL_KEY))));
        noAnswerKeys(detail); // Exhausting retries still does not reveal the answer key.
        mvc.perform(post(base(fixture)).header("Authorization", TestTokens.bearer(fixture.student()))
                .contentType(MediaType.APPLICATION_JSON).content(submission(fixture, true)))
            .andExpect(status().isConflict());
        assertEquals(2, attempts.countByEnrollment_IdAndQuiz_Id(fixture.studentEnrollmentId(), fixture.quizId()));
    }

    @Test
    void queryParametersCannotImpersonateAnotherStudent() throws Exception {
        Fixture fixture = fixture();
        Long id = submit(fixture, false);
        String parameters = "?userId=" + fixture.student().getUserId() + "&enrollmentId=" + fixture.studentEnrollmentId();
        getAs(base(fixture) + "/me/count" + parameters, fixture.peer()).andExpect(status().isOk())
            .andExpect(jsonPath("$.usedAttempts").value(0));
        getAs(base(fixture) + "/me" + parameters, fixture.peer()).andExpect(status().isOk())
            .andExpect(content().json("[]"));
        getAs(base(fixture) + "/" + id + parameters, fixture.peer()).andExpect(status().isNotFound());
    }

    @Test
    void wrongQuizMissingAttemptAndMissingQuizAreNotFound() throws Exception {
        Fixture fixture = fixture();
        Long id = submit(fixture, false);
        List<Long> otherIds = tx(() -> {
            Quiz original = entities.find(Quiz.class, fixture.quizId());
            Quiz other = save(new Quiz(original.getModule(), "Other quiz", 1, 2));
            StaticPage page = save(new StaticPage(original.getModule(), "Page", 2, "Body"));
            return List.of(other.getId(), page.getId());
        });
        getAs("/api/quizzes/" + otherIds.get(0) + "/attempts/" + id, fixture.student()).andExpect(status().isNotFound());
        getAs(base(fixture) + "/" + Long.MAX_VALUE, fixture.student()).andExpect(status().isNotFound());
        for (Long quizId : List.of(Long.MAX_VALUE, otherIds.get(1))) {
            for (String suffix : List.of("me/count", "me", id.toString())) {
                getAs("/api/quizzes/" + quizId + "/attempts/" + suffix, fixture.student()).andExpect(status().isNotFound());
            }
        }
    }

    @Test
    void realFiltersRejectUnauthenticatedRequests() throws Exception {
        Fixture fixture = fixture();
        Long id = submit(fixture, false);
        for (String suffix : List.of("me/count", "me", id.toString())) {
            mvc.perform(get(base(fixture) + "/" + suffix)).andExpect(status().isUnauthorized());
            mvc.perform(get(base(fixture) + "/" + suffix).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void teacherOtherCourseAndInactiveStudentsCannotUseStudentQueries() throws Exception {
        Fixture fixture = fixture(), other = fixture();
        Long id = submit(fixture, false);
        tx(() -> {
            entities.find(Enrollment.class, fixture.studentEnrollmentId()).deactivate();
            return null;
        });
        for (User denied : List.of(fixture.teacher(), other.student(), fixture.student())) {
            for (String suffix : List.of("me/count", "me", id.toString())) {
                getAs(base(fixture) + "/" + suffix, denied).andExpect(status().isForbidden());
            }
        }
        assertEquals(1, attempts.countByEnrollment_IdAndQuiz_Id(fixture.studentEnrollmentId(), fixture.quizId()));
        assertEquals(5, answers.findByAttempt_IdOrderByQuestion_PositionAsc(id).size());
    }

    @Test
    void invalidSubmissionDoesNotIncreaseUsedCount() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(base(fixture)).header("Authorization", TestTokens.bearer(fixture.student()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[]}"))
            .andExpect(status().isBadRequest());
        getAs(base(fixture) + "/me/count", fixture.student()).andExpect(status().isOk())
            .andExpect(jsonPath("$.usedAttempts").value(0));
    }
}
