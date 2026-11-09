
package no.hvl.dat250.lms.content.service;

import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.domain.QuizQuestion;
import no.hvl.dat250.lms.content.domain.ChoiceOption;

import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.repository.QuizQuestionRepository;
import no.hvl.dat250.lms.content.repository.ChoiceOptionRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final ContentRepository contentRepository;
    private final QuizQuestionRepository questionRepository;
    private final ChoiceOptionRepository optionRepository;
    private final ModuleService moduleService;

    public QuizService(
            ContentRepository contentRepository,
            QuizQuestionRepository questionRepository,
            ChoiceOptionRepository optionRepository,
            ModuleService moduleService) {

        this.contentRepository = contentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.moduleService = moduleService;
    }

    // Data needed to create one option.
    // This is a service-level record, not an HTTP DTO.
    public record OptionDraft(
            String optionText,
            int position,
            boolean correct
    ) {}

    //  Find Quiz
        public record QuestionWithOptions(
        QuizQuestion question,
        List<ChoiceOption> options
    ) {}

    public record QuizWithQuestions(
        Quiz quiz,
        List<QuestionWithOptions> questions
    ) {}


    @Transactional
    public Quiz createQuiz(
            Long courseId,
            Long moduleId,
            String title,
            int position,
            int maxAttempts) {

        if (courseId == null || moduleId == null) {
            throw new IllegalArgumentException(
                "Course ID and Module ID are required");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Quiz title must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Position must not be negative");
        }

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                "Max attempts must be at least 1");
        }

        Module module = moduleService
                .findModuleInCourse(courseId, moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Module not found in course"));

        Quiz quiz = new Quiz(
            module, title, position, maxAttempts
        );

        return contentRepository.save(quiz);
    }

    @Transactional
    public QuizQuestion addQuestion(
            Long quizId,
            String questionText,
            int position,
            String hint,
            List<OptionDraft> options) {

        if (quizId == null) {
            throw new IllegalArgumentException(
                "Quiz ID is required");
        }

        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException(
                "Question text must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Question position must not be negative");
        }

        validateOptions(options);

        Content content = contentRepository.findById(quizId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Quiz not found: " + quizId));

        if (!(content instanceof Quiz quiz)) {
            throw new NoSuchElementException(
                "Quiz not found: " + quizId);
        }

        QuizQuestion question = questionRepository.save(
            new QuizQuestion(
                quiz, questionText, position, hint
            )
        );

        for (OptionDraft option : options) {
            optionRepository.save(
                new ChoiceOption(
                    question,
                    option.optionText(),
                    option.position(),
                    option.correct()
                )
            );
        }

        return question;
    }

    private void validateOptions(List<OptionDraft> options) {

        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException(
                "A question needs at least two options");
        }

        boolean hasCorrectOption = false;

        for (OptionDraft option : options) {

            if (option == null) {
                throw new IllegalArgumentException(
                    "Option must not be null");
            }

            if (option.optionText() == null ||
                    option.optionText().isBlank()) {
                throw new IllegalArgumentException(
                    "Option text must not be blank");
            }

            if (option.position() < 0) {
                throw new IllegalArgumentException(
                    "Option position must not be negative");
            }

            if (option.correct()) {
                hasCorrectOption = true;
            }
        }

        if (!hasCorrectOption) {
            throw new IllegalArgumentException(
                "At least one option must be correct");
        }
    }

    @Transactional(readOnly = true)
    public QuizWithQuestions findQuizWithQuestions(Long quizId) {

        if (quizId == null) {
            throw new IllegalArgumentException(
                "Quiz ID is required"
            );
        }

        Content content = contentRepository.findById(quizId)
            .orElseThrow(() -> new NoSuchElementException(
                "Quiz not found: " + quizId
            ));

        if (!(content instanceof Quiz quiz)) {
            throw new NoSuchElementException(
                "Quiz not found: " + quizId
            );
        }

        List<QuestionWithOptions> questions =
            questionRepository
                .findByQuiz_IdOrderByPositionAsc(quizId)
                .stream()
                .map(question -> new QuestionWithOptions(
                    question,
                    optionRepository
                        .findByQuestion_IdOrderByPositionAsc(
                            question.getId()
                        )
                ))
                .toList();

        return new QuizWithQuestions(quiz, questions);
    }
}
