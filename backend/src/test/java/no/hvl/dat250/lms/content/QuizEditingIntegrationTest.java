package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PUT /api/quizzes/{id} (title, maxAttempts) and PUT /api/quizzes/{id}/questions. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz_editing_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizEditingIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;
    @Autowired private QuizService quizzes;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(User teacher, User student, Quiz quiz) {}

    private Fixture fixture() {
        Course course = save(new Course("DAT250", "Quiz editing", Term.AUTUMN, 2026));
        User teacher = user();
        User student = user();
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        save(new Enrollment(student, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week", 0));
        Quiz quiz = save(new Quiz(module, "Quiz", 0, 3));
        quizzes.addQuestion(quiz.getId(), "Old question", 0, null,
            List.of(new OptionDraft("Yes", 0, true), new OptionDraft("No", 1, false)));
        return new Fixture(teacher, student, quiz);
    }

    private static final String NEW_QUESTIONS = """
        {"questions":[
          {"questionText":"Pick B","position":0,"hint":"Not A","type":"MULTIPLE_CHOICE",
           "options":[{"optionText":"A","position":0,"correct":false},{"optionText":"B","position":1,"correct":true}]},
          {"questionText":"Language?","position":1,"type":"FILL_IN_BLANK","acceptedAnswers":["Java"]},
          {"questionText":"Order","position":2,"type":"ORDERING",
           "ordering":{"items":[{"key":"a","text":"First"},{"key":"b","text":"Second"}],"correctOrder":["a","b"]}},
          {"questionText":"Match","position":3,"type":"MATCHING",
           "matching":{"leftItems":[{"key":"l1","text":"1"},{"key":"l2","text":"2"}],
                       "rightItems":[{"key":"r1","text":"One"},{"key":"r2","text":"Two"}],
                       "correctMatches":[{"leftKey":"l1","rightKey":"r1"},{"leftKey":"l2","rightKey":"r2"}]}}
        ]}
        """;

    @Test
    void teacherChangesTitleAndAttemptsAndKeepsAttemptsWhenLeftOut() throws Exception {
        Fixture f = fixture();
        String token = TestTokens.bearer(f.teacher());
        mvc.perform(put("/api/quizzes/{id}", f.quiz().getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Renamed\",\"maxAttempts\":5}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Renamed"))
            .andExpect(jsonPath("$.maxAttempts").value(5));
        mvc.perform(put("/api/quizzes/{id}", f.quiz().getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Again\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.maxAttempts").value(5));
        mvc.perform(put("/api/quizzes/{id}", f.quiz().getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Zero\",\"maxAttempts\":0}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/quizzes/{id}", f.quiz().getId()).header("Authorization", TestTokens.bearer(f.student()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Mine\",\"maxAttempts\":99}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void replacingQuestionsSwapsTheWholeAnswerKey() throws Exception {
        Fixture f = fixture();
        String token = TestTokens.bearer(f.teacher());
        mvc.perform(put("/api/quizzes/{id}/questions", f.quiz().getId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(NEW_QUESTIONS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.questions", hasSize(4)))
            .andExpect(jsonPath("$.questions[0].questionText").value("Pick B"));
        mvc.perform(get("/api/quizzes/{id}/answer-key", f.quiz().getId()).header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.questions", hasSize(4)))
            .andExpect(jsonPath("$.questions[0].hint").value("Not A"))
            .andExpect(jsonPath("$.questions[1].acceptedAnswers[0]").value("Java"))
            .andExpect(jsonPath("$.questions[2].correctOrder", hasSize(2)))
            .andExpect(jsonPath("$.questions[3].correctMatches", hasSize(2)));
        entities.clear();
        assertEquals(4, quizzes.findQuizWithQuestions(f.quiz().getId()).questions().size());
    }

    @Test
    void invalidQuestionsChangeNothing() throws Exception {
        Fixture f = fixture();
        mvc.perform(put("/api/quizzes/{id}/questions", f.quiz().getId())
                .header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questions\":[{\"questionText\":\"No correct\",\"position\":0,"
                    + "\"options\":[{\"optionText\":\"A\",\"position\":0,\"correct\":false},"
                    + "{\"optionText\":\"B\",\"position\":1,\"correct\":false}]}]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/quizzes/{id}/questions", f.quiz().getId())
                .header("Authorization", TestTokens.bearer(f.student()))
                .contentType(MediaType.APPLICATION_JSON).content(NEW_QUESTIONS))
            .andExpect(status().isForbidden());
    }

    @Test
    void questionsCannotBeReplacedOnceAStudentHasAnAttempt() throws Exception {
        Fixture f = fixture();
        Long questionId = quizzes.findQuizWithQuestions(f.quiz().getId()).questions().getFirst().question().getId();
        Long correct = quizzes.findQuizWithQuestions(f.quiz().getId()).questions().getFirst().options().getFirst().getId();
        mvc.perform(post("/api/quizzes/{id}/attempts", f.quiz().getId())
                .header("Authorization", TestTokens.bearer(f.student()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":" + questionId + ",\"selectedOptionIds\":[" + correct + "]}]}"))
            .andExpect(status().isCreated());
        mvc.perform(put("/api/quizzes/{id}/questions", f.quiz().getId())
                .header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(NEW_QUESTIONS))
            .andExpect(status().isConflict());
        // The number of attempts can still be changed.
        mvc.perform(put("/api/quizzes/{id}", f.quiz().getId()).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Quiz\",\"maxAttempts\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.maxAttempts").value(1));
    }
}
