
package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;

import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.*;
import no.hvl.dat250.lms.content.service.QuizAttemptService;
import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator.AnswerInput;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizAttemptServiceIntegrationTest {

    @Autowired EntityManager entityManager;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired ModuleRepository moduleRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ContentRepository contentRepository;
    @Autowired QuizQuestionRepository questionRepository;
    @Autowired ChoiceOptionRepository optionRepository;
    @Autowired QuizAttemptRepository attemptRepository;
    @Autowired QuizAnswerRepository answerRepository;

    @Autowired QuizAttemptService attemptService;

    private record Fixture(
            Long userId,
            Quiz quiz,
            Enrollment enrollment,
            QuizQuestion q1,
            QuizQuestion q2,
            ChoiceOption correctOption
    ) {}

    private Fixture createFixture() {

        // 1. Create a User
        User user = userRepository.save(
            new User(
                "quiz_it_alice@example.com",
                "Alice",
                "Test"
            )
        );

        entityManager.flush();

        // User does not currently expose getId().
        Long userId = ((Number) entityManager
            .getEntityManagerFactory()
            .getPersistenceUnitUtil()
            .getIdentifier(user)).longValue();

        // 2. Create Course and Enrollment
        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Test course",
                Term.AUTUMN,
                2026
            )
        );

        Enrollment enrollment =
            enrollmentRepository.saveAndFlush(
                new Enrollment(
                    user,
                    course,
                    CourseRole.STUDENT
                )
            );

        // 3. Create Module and Quiz
        Module module = moduleRepository.saveAndFlush(
            new Module(course, "Week 1", 0)
        );

        Quiz quiz = contentRepository.saveAndFlush(
            new Quiz(module, "Java Quiz", 0, 2)
        );

        // 4. Create Questions
        QuizQuestion q1 = questionRepository.saveAndFlush(
            new QuizQuestion(
                quiz,
                "Is Java object-oriented?",
                0,
                null
            )
        );

        QuizQuestion q2 = questionRepository.saveAndFlush(
            new QuizQuestion(
                quiz,
                "Choose the correct statement",
                1,
                null
            )
        );

        // 5. Create Options
        ChoiceOption optionA = optionRepository.saveAndFlush(
            new ChoiceOption(q1, "Yes", 0, true)
        );

        optionRepository.saveAndFlush(
            new ChoiceOption(q1, "No", 1, false)
        );

        optionRepository.saveAndFlush(
            new ChoiceOption(q2, "Correct", 0, true)
        );

        optionRepository.saveAndFlush(
            new ChoiceOption(q2, "Incorrect", 1, false)
        );

        return new Fixture(
            userId,
            quiz,
            enrollment,
            q1,
            q2,
            optionA
        );
    }

    @Test
    void submissionIsGradedAndPersisted() {

        Fixture f = createFixture();

        // Submit one correct answer and one empty answer.
        QuizAttempt attempt = attemptService.submitAttempt(
            f.quiz().getId(),
            f.userId(),
            List.of(
                new AnswerInput(
                    f.q1().getId(),
                    List.of(f.correctOption().getId())
                ),
                new AnswerInput(
                    f.q2().getId(),
                    List.of()
                )
            )
        );

        assertEquals(1, attempt.getCorrectCount());
        assertEquals(2, attempt.getTotalQuestions());

        Long attemptId = attempt.getId();
        assertNotNull(attemptId);

        // Force SQL synchronization, then clear cached entities.
        entityManager.flush();
        entityManager.clear();

        // Reload from the database.
        QuizAttempt stored = attemptRepository
            .findById(attemptId)
            .orElseThrow();

        assertEquals(1, stored.getCorrectCount());
        assertEquals(2, stored.getTotalQuestions());

        assertEquals(
            f.enrollment().getId(),
            stored.getEnrollment().getId()
        );

        // Verify individual answers.
        List<QuizAnswer> answers = answerRepository
            .findByAttempt_IdOrderByQuestion_PositionAsc(
                attemptId
            );

        assertEquals(2, answers.size());

        QuizAnswer firstAnswer = answers.get(0);

        assertEquals(
            f.q1().getId(),
            firstAnswer.getQuestion().getId()
        );

        assertTrue(firstAnswer.isCorrect());

        Set<Long> selectedIds = firstAnswer
            .getSelectedOptions()
            .stream()
            .map(ChoiceOption::getId)
            .collect(Collectors.toSet());

        assertEquals(
            Set.of(f.correctOption().getId()),
            selectedIds
        );

        // Second answer was submitted with no selected options.
        QuizAnswer secondAnswer = answers.get(1);

        assertFalse(secondAnswer.isCorrect());
        assertTrue(
            secondAnswer.getSelectedOptions().isEmpty()
        );

        // Verify that this attempt is counted.
        assertEquals(
            1L,
            attemptRepository.countByEnrollment_IdAndQuiz_Id(
                f.enrollment().getId(),
                f.quiz().getId()
            )
        );
    }

    @Test
    void invalidSubmissionDoesNotSaveAttempt() {

        Fixture f = createFixture();

        // Option ID 999999 does not belong to this question.
        assertThrows(
            IllegalArgumentException.class,
            () -> attemptService.submitAttempt(
                f.quiz().getId(),
                f.userId(),
                List.of(
                    new AnswerInput(
                        f.q1().getId(),
                        List.of(999999L)
                    ),
                    new AnswerInput(
                        f.q2().getId(),
                        List.of()
                    )
                )
            )
        );

        assertEquals(
            0L,
            attemptRepository.countByEnrollment_IdAndQuiz_Id(
                f.enrollment().getId(),
                f.quiz().getId()
            )
        );
    }
}
