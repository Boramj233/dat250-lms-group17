
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DataJpaTest
class QuizAttemptRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private ChoiceOptionRepository optionRepository;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private QuizAnswerRepository answerRepository;

    @Test
    void savesAttemptAndSelectedOptions() {

        // 1. Set up User, Course and Enrollment
        User user = userRepository.save(
            new User(
                "alice",
                "alice@example.com",
                "test-password"
            )
        );

        entityManager.flush();

        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Test course",
                Term.AUTUMN,
                2026
            )
        );

        Enrollment enrollment = enrollmentRepository.saveAndFlush(
            new Enrollment(user, course, CourseRole.STUDENT)
        );

        // 2. Create Module and Quiz
        Module module = moduleRepository.saveAndFlush(
            new Module(course, "Week 1", 0)
        );

        Quiz quiz = contentRepository.saveAndFlush(
            new Quiz(module, "Java Quiz", 0, 2)
        );

        // 3. Save two questions
        QuizQuestion q1 = questionRepository.saveAndFlush(
            new QuizQuestion(quiz, "Question 1", 0, null)
        );

        QuizQuestion q2 = questionRepository.saveAndFlush(
            new QuizQuestion(quiz, "Question 2", 1, null)
        );

        // 4. Save options
        ChoiceOption optionA = optionRepository.saveAndFlush(
            new ChoiceOption(q1, "A", 0, true)
        );

        ChoiceOption optionB = optionRepository.saveAndFlush(
            new ChoiceOption(q1, "B", 1, true)
        );

        optionRepository.saveAndFlush(
            new ChoiceOption(q2, "C", 0, true)
        );

        optionRepository.saveAndFlush(
            new ChoiceOption(q2, "D", 1, false)
        );

        // 5. Save the attempt (1 out of 2 correct)
        QuizAttempt attempt = attemptRepository.saveAndFlush(
            new QuizAttempt(
                quiz,
                enrollment,
                Instant.parse("2026-11-08T12:00:00Z"),
                1,
                2
            )
        );

        // 6. First answer selects two options
        answerRepository.saveAndFlush(
            new QuizAnswer(
                attempt,
                q1,
                Set.of(optionA, optionB),
                true
            )
        );

        // Second answer selects nothing
        answerRepository.saveAndFlush(
            new QuizAnswer(
                attempt,
                q2,
                Set.of(),
                false
            )
        );

        Long attemptId = attempt.getId();

        entityManager.flush();
        entityManager.clear();

        // 7. Read the attempt from the database
        QuizAttempt found = attemptRepository
            .findById(attemptId)
            .orElseThrow();

        assertEquals(1, found.getCorrectCount());
        assertEquals(2, found.getTotalQuestions());

        assertEquals(
            "alice@example.com",
            found.getEnrollment().getUser().getEmail()
        );

        assertEquals(
            quiz.getId(),
            found.getQuiz().getId()
        );

        // 8. Read the answers
        List<QuizAnswer> answers = answerRepository
            .findByAttempt_IdOrderByQuestion_PositionAsc(
                attemptId
            );

        assertEquals(2, answers.size());

        assertTrue(answers.get(0).isCorrect());
        assertEquals(
            2,
            answers.get(0).getSelectedOptions().size()
        );

        assertFalse(answers.get(1).isCorrect());
        assertTrue(
            answers.get(1).getSelectedOptions().isEmpty()
        );

        // 9. Verify attempt counting
        long count = attemptRepository
            .countByEnrollment_IdAndQuiz_Id(
                enrollment.getId(),
                quiz.getId()
            );

        assertEquals(1L, count);
    }


    @Test
    void quizAnswerAcceptsImmutableSetOfOptions() {
        QuizAttempt attempt = mock(QuizAttempt.class);
        QuizQuestion question = mock(QuizQuestion.class);

        ChoiceOption optionA = mock(ChoiceOption.class);
        ChoiceOption optionB = mock(ChoiceOption.class);

        QuizAnswer answer = new QuizAnswer(
            attempt,
            question,
            Set.of(optionA, optionB),
            true
        );

        assertEquals(2, answer.getSelectedOptions().size());
    }
}
