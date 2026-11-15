package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import no.hvl.dat250.lms.content.repository.QuizAnswerRepository;
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

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real filters, JWT converter, enrollment checks, grading and persistence. The existing
 * TestJwtDecoderConfig validates signed test tokens locally; no live Keycloak is used.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz_security_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizAttemptSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager entities;
    @Autowired QuizAttemptRepository attempts;
    @Autowired QuizAnswerRepository answers;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User("quiz_" + UUID.randomUUID() + "@example.com", "Test", "Student"));
    }

    private Course course() {
        return save(new Course("DAT250", "Quiz security", Term.AUTUMN, 2026));
    }

    private record Fixture(User user, Course course, Enrollment enrollment, Quiz quiz,
                           QuizQuestion question, ChoiceOption correct) {}

    private Fixture fixture() {
        User user = user();
        Course course = course();
        Enrollment enrollment = save(new Enrollment(user, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week 1", 0));
        Quiz quiz = save(new Quiz(module, "Java", 0, 1));
        QuizQuestion question = save(new QuizQuestion(quiz, "Select correct", 0, null));
        ChoiceOption correct = save(new ChoiceOption(question, "Correct", 0, true));
        save(new ChoiceOption(question, "Incorrect", 1, false));
        return new Fixture(user, course, enrollment, quiz, question, correct);
    }

    private String body(Fixture f, long optionId) {
        return "{\"answers\":[{\"questionId\":" + f.question().getId()
            + ",\"selectedOptionIds\":[" + optionId + "]}]}";
    }

    private MockHttpServletRequestBuilder submit(Fixture f, User user) {
        return post("/api/quizzes/{id}/attempts", f.quiz().getId())
            .header("Authorization", TestTokens.bearer(user))
            .contentType(MediaType.APPLICATION_JSON).content(body(f, f.correct().getId()));
    }

    @Test
    void missingAndInvalidTokensReturn401() throws Exception {
        mvc.perform(post("/api/quizzes/1/attempts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[]}"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
            .andExpect(header().doesNotExist("Location"));
        mvc.perform(post("/api/quizzes/1/attempts").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        assertEquals(0, attempts.count());
    }

    @Test
    void expiredTokenCannotCreateAttempt() throws Exception {
        var f = fixture();
        String token = TestTokens.accessToken(claims -> claims.claim("email", f.user().getEmail())
            .issuedAt(Instant.now().minusSeconds(600)).expiresAt(Instant.now().minusSeconds(120)));
        mvc.perform(post("/api/quizzes/{id}/attempts", f.quiz().getId())
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(body(f, f.correct().getId())))
            .andExpect(status().isUnauthorized());
        assertEquals(0, attempts.count());
    }

    @Test
    void validStudentIsGradedAndStoredWithTokenOwnersEnrollment() throws Exception {
        var f = fixture();
        User other = user();
        mvc.perform(submit(f, f.user()).param("userId", other.getUserId().toString()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.correctCount").value(1))
            .andExpect(jsonPath("$.totalQuestions").value(1));
        entities.flush();
        entities.clear();
        QuizAttempt saved = attempts.findAll().getFirst();
        assertEquals(f.enrollment().getId(), saved.getEnrollment().getId());
        assertEquals(f.quiz().getId(), saved.getQuiz().getId());
        assertEquals(1, saved.getCorrectCount());
        var storedAnswers = answers.findByAttempt_IdOrderByQuestion_PositionAsc(saved.getId());
        assertEquals(1, storedAnswers.size());
        assertTrue(storedAnswers.getFirst().isCorrect());
        assertEquals(f.correct().getId(), storedAnswers.getFirst().getSelectedOptions().iterator().next().getId());
    }

    @Test
    void studentFromAnotherCourseIsForbidden() throws Exception {
        var f = fixture();
        User outsider = user();
        save(new Enrollment(outsider, course(), CourseRole.STUDENT));
        mvc.perform(submit(f, outsider)).andExpect(status().isForbidden());
        assertEquals(0, attempts.count());
        assertEquals(0, answers.count());
    }

    @Test
    void inactiveStudentIsForbidden() throws Exception {
        var f = fixture();
        f.enrollment().deactivate();
        entities.flush();
        mvc.perform(submit(f, f.user())).andExpect(status().isForbidden());
        assertEquals(0, attempts.count());
    }

    @Test
    void courseTeacherCannotTakeStudentAttempt() throws Exception {
        var f = fixture();
        User teacher = user();
        save(new Enrollment(teacher, f.course(), CourseRole.TEACHER));
        mvc.perform(submit(f, teacher)).andExpect(status().isForbidden());
        assertEquals(0, attempts.count());
    }

    @Test
    void exhaustedAttemptsReturn409WithoutAdditionalAttemptOrAnswer() throws Exception {
        var f = fixture();
        mvc.perform(submit(f, f.user())).andExpect(status().isCreated());
        mvc.perform(submit(f, f.user())).andExpect(status().isConflict());
        assertEquals(1, attempts.count());
        assertEquals(1, answers.count());
    }

    @Test
    void invalidAnswerReturns400AndSavesNothing() throws Exception {
        var f = fixture();
        mvc.perform(submit(f, f.user()).content(body(f, 999999)))
            .andExpect(status().isBadRequest());
        assertEquals(0, attempts.count());
        assertEquals(0, answers.count());
    }

    @Test
    void missingQuizReturns404() throws Exception {
        var f = fixture();
        mvc.perform(post("/api/quizzes/999999/attempts")
                .header("Authorization", TestTokens.bearer(f.user()))
                .contentType(MediaType.APPLICATION_JSON).content(body(f, f.correct().getId())))
            .andExpect(status().isNotFound());
    }
}
