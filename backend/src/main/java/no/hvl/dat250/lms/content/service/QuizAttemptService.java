
package no.hvl.dat250.lms.content.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Set;

import no.hvl.dat250.lms.content.domain.ChoiceOption;
import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.domain.QuizAnswer;
import no.hvl.dat250.lms.content.domain.QuizAttempt;
import no.hvl.dat250.lms.content.domain.QuizItem;
import java.util.UUID;

import no.hvl.dat250.lms.content.repository.QuizAnswerRepository;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;

import no.hvl.dat250.lms.content.service.QuizSubmissionEvaluator.AnswerInput;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;

import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizAttemptService {

    private final QuizService quizService;
    private final EnrollmentService enrollmentService;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;

    private final QuizSubmissionEvaluator evaluator =
            new QuizSubmissionEvaluator();

    public QuizAttemptService(
            QuizService quizService,
            EnrollmentService enrollmentService,
            QuizAttemptRepository attemptRepository,
            QuizAnswerRepository answerRepository) {

        this.quizService = quizService;
        this.enrollmentService = enrollmentService;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    public record QuestionHint(Long questionId, String hint) {
    }

    public record AttemptResult(QuizAttempt attempt, List<QuestionHint> hints) {
    }

    @Transactional
    public QuizAttempt submitAttempt(
            Long quizId,
            Long userId,
            List<AnswerInput> answers) {

        return submitAttemptWithFeedback(quizId, userId, answers).attempt();
    }

    @Transactional
    public AttemptResult submitAttemptWithFeedback(
            Long quizId,
            Long userId,
            List<AnswerInput> answers) {

        if (quizId == null || quizId <= 0 ||
                userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                "Valid Quiz ID and User ID are required");
        }

        // 1. Load the complete Quiz.
        var details = quizService.findQuizWithQuestions(quizId);

        Quiz quiz = details.quiz();

        Long courseId = quiz.getModule()
                .getCourse()
                .getId();

        // 2. Verify the student's enrollment.
        Enrollment enrollment;
        try {
            enrollment = enrollmentService.requireActiveStudent(userId, courseId);
        } catch (NoSuchElementException | IllegalStateException ex) {
            // Translate only the enrollment operation's documented membership refusals.
            throw new AccessDeniedException("Active student enrollment required", ex);
        }

        // 3. Check how many attempts are already stored.
        long previousAttempts = attemptRepository
                .countByEnrollment_IdAndQuiz_Id(
                    enrollment.getId(),
                    quizId
                );

        if (previousAttempts >= quiz.getMaxAttempts()) {
            throw new QuizAttemptLimitExceededException();
        }

        // 4. Validate the submission and calculate score.
        var result = evaluator.evaluate(details, answers);

        // 5. Save the overall attempt.
        QuizAttempt attempt = attemptRepository.save(
            new QuizAttempt(
                quiz,
                enrollment,
                Instant.now(),
                result.correctCount(),
                result.totalQuestions()
            )
        );

        // Create a lookup from question ID to its details.
        Map<Long, QuizService.QuestionWithOptions> questions =
                new HashMap<>();

        for (var item : details.questions()) {
            questions.put(item.question().getId(), item);
        }

        List<QuestionHint> hints = new ArrayList<>();

        // 6. Save each individual answer.
        for (var graded : result.answers()) {

            var item = questions.get(graded.questionId());

            Set<Long> selectedIds =
                    new HashSet<>(graded.selectedOptionIds());

            Set<ChoiceOption> selectedOptions =
                    new HashSet<>();

            for (ChoiceOption option : item.options()) {
                if (selectedIds.contains(option.getId())) {
                    selectedOptions.add(option);
                }
            }

            Map<UUID, QuizItem> itemsById = new HashMap<>();
            for (QuizItem questionItem : item.question().getItems()) {
                itemsById.put(questionItem.getId(), questionItem);
            }
            List<QuizItem> orderedItems = graded.orderedItemIds().stream().map(itemsById::get).toList();

            QuizAnswer answer = new QuizAnswer(
                attempt,
                item.question(),
                selectedOptions,
                graded.correct(),
                graded.textAnswer(),
                orderedItems
            );

            answerRepository.save(answer);
            String hint = item.question().getHint();
            if (!graded.correct() && hint != null && !hint.isBlank()) {
                hints.add(new QuestionHint(graded.questionId(), hint));
            }
        }

        // 7. Transaction commits when the method succeeds.
        return new AttemptResult(attempt, List.copyOf(hints));
    }
}
