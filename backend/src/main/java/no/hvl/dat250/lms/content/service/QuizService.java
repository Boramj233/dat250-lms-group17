
package no.hvl.dat250.lms.content.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import no.hvl.dat250.lms.content.domain.QuizItem;
import no.hvl.dat250.lms.content.domain.QuizItemKind;
import no.hvl.dat250.lms.content.domain.QuizMatch;
import no.hvl.dat250.lms.content.repository.QuizItemRepository;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.QuestionType;
import no.hvl.dat250.lms.content.repository.QuizAttemptRepository;
import org.hibernate.Hibernate;
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
    private final QuizAttemptRepository attemptRepository;
    private final QuizItemRepository itemRepository;

    public QuizService(
            ContentRepository contentRepository,
            QuizQuestionRepository questionRepository,
            ChoiceOptionRepository optionRepository,
            ModuleService moduleService,
            QuizAttemptRepository attemptRepository,
            QuizItemRepository itemRepository) {

        this.contentRepository = contentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.moduleService = moduleService;
        this.attemptRepository = attemptRepository;
        this.itemRepository = itemRepository;
    }

    // Data needed to create one option.
    // This is a service-level record, not an HTTP DTO.
    public record OptionDraft(
            String optionText,
            int position,
            boolean correct
    ) {}

    public record ItemDraft(String key, String text) {
    }

    public record MatchDraft(String leftKey, String rightKey) {
    }

    public record MatchingDraft(List<ItemDraft> leftItems, List<ItemDraft> rightItems,
                                List<MatchDraft> correctMatches) {
    }

    public record OrderingDraft(List<ItemDraft> items, List<String> correctOrder) {
    }

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

        if (contentRepository.existsByModule_IdAndPosition(moduleId, position)) {
            throw new ContentPositionConflictException("Content position is already occupied in this module");
        }

        return contentRepository.save(quiz);
    }

    @Transactional
    public QuizQuestion addQuestion(
            Long quizId,
            String questionText,
            int position,
            String hint,
            List<OptionDraft> options) {

        return addQuestion(quizId, questionText, position, hint, options,
            QuestionType.MULTIPLE_CHOICE, List.of());
    }

    @Transactional
    public QuizQuestion addQuestion(
            Long quizId, String questionText, int position, String hint,
            List<OptionDraft> options, QuestionType type, List<String> acceptedAnswers) {

        return addQuestion(quizId, questionText, position, hint, options, type, acceptedAnswers, null);
    }

    @Transactional
    public QuizQuestion addQuestion(
            Long quizId, String questionText, int position, String hint,
            List<OptionDraft> options, QuestionType type, List<String> acceptedAnswers,
            OrderingDraft ordering) {
        return addQuestion(quizId, questionText, position, hint, options, type, acceptedAnswers, ordering, null);
    }

    @Transactional
    public QuizQuestion addQuestion(
            Long quizId, String questionText, int position, String hint,
            List<OptionDraft> options, QuestionType type, List<String> acceptedAnswers,
            OrderingDraft ordering, MatchingDraft matching) {

        if (quizId == null || quizId <= 0) {
            throw new IllegalArgumentException("Positive Quiz ID is required");
        }

        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException(
                "Question text must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Question position must not be negative");
        }

        if (type == null) {
            throw new IllegalArgumentException("Question type is required");
        }
        if (type == QuestionType.ORDERING) {
            validateOrdering(ordering);
        } else if (ordering != null) {
            throw new IllegalArgumentException("Only ordering questions may contain an ordering definition");
        }
        if (type == QuestionType.MATCHING) {
            validateMatching(matching);
        } else if (matching != null) {
            throw new IllegalArgumentException("Only matching questions may contain a matching definition");
        }
        if (type == QuestionType.FILL_IN_BLANK || type == QuestionType.ORDERING || type == QuestionType.MATCHING) {
            if (options != null && !options.isEmpty()) {
                throw new IllegalArgumentException("Fill-in, ordering and matching questions must not contain options");
            }
            options = List.of();
        } else {
            validateOptions(options);
            if (type == QuestionType.TRUE_FALSE && (options.size() != 2
                    || options.stream().filter(OptionDraft::correct).count() != 1)) {
                throw new IllegalArgumentException("True/false questions need two options and exactly one correct option");
            }
        }

        Content content = contentRepository.findById(quizId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Quiz not found: " + quizId));

        if (!(Hibernate.unproxy(content) instanceof Quiz quiz)) {
            throw new NoSuchElementException(
                "Quiz not found: " + quizId);
        }

        if (attemptRepository.existsByQuiz_Id(quizId)) {
            throw new QuizQuestionConflictException();
        }

        QuizQuestion question = questionRepository.save(
            new QuizQuestion(
                quiz, questionText, position, hint, type, acceptedAnswers
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

        if (type == QuestionType.ORDERING) {
            Map<String, QuizItem> itemsByKey = new LinkedHashMap<>();
            for (ItemDraft draft : ordering.items()) {
                itemsByKey.put(draft.key(), itemRepository.save(new QuizItem(question, draft.text())));
            }
            List<QuizItem> correctOrder = ordering.correctOrder().stream().map(itemsByKey::get).toList();
            question.configureOrdering(new ArrayList<>(itemsByKey.values()), correctOrder);
        }
        if (type == QuestionType.MATCHING) {
            Map<String, QuizItem> leftItems = saveMatchingItems(question, matching.leftItems(), QuizItemKind.MATCHING_LEFT);
            Map<String, QuizItem> rightItems = saveMatchingItems(question, matching.rightItems(), QuizItemKind.MATCHING_RIGHT);
            List<QuizItem> items = new ArrayList<>(leftItems.values());
            items.addAll(rightItems.values());
            List<QuizMatch> correctMatches = matching.correctMatches().stream()
                .map(pair -> new QuizMatch(leftItems.get(pair.leftKey()), rightItems.get(pair.rightKey())))
                .toList();
            question.configureMatching(items, correctMatches);
        }
        return question;
    }

    private Map<String, QuizItem> saveMatchingItems(QuizQuestion question, List<ItemDraft> drafts, QuizItemKind kind) {
        Map<String, QuizItem> itemsByKey = new LinkedHashMap<>();
        for (ItemDraft draft : drafts) {
            itemsByKey.put(draft.key(), itemRepository.save(new QuizItem(question, draft.text(), kind)));
        }
        return itemsByKey;
    }

    private void validateMatching(MatchingDraft matching) {
        if (matching == null || matching.correctMatches() == null) {
            throw new IllegalArgumentException("Matching definition and correct matches are required");
        }
        var leftKeys = validateMatchingSide(matching.leftItems());
        var rightKeys = validateMatchingSide(matching.rightItems());
        if (leftKeys.size() != rightKeys.size() || matching.correctMatches().size() != leftKeys.size()) {
            throw new IllegalArgumentException("Matching sides and pairs must have equal size");
        }
        var usedLeft = new HashSet<String>();
        var usedRight = new HashSet<String>();
        for (MatchDraft pair : matching.correctMatches()) {
            if (pair == null || !leftKeys.contains(pair.leftKey()) || !rightKeys.contains(pair.rightKey())
                    || !usedLeft.add(pair.leftKey()) || !usedRight.add(pair.rightKey())) {
                throw new IllegalArgumentException("Correct matches must use every left and right key exactly once");
            }
        }
    }

    private Set<String> validateMatchingSide(List<ItemDraft> items) {
        if (items == null || items.size() < 2 || items.size() > 20) {
            throw new IllegalArgumentException("Each matching side requires 2–20 items");
        }
        var keys = new HashSet<String>();
        var labels = new HashSet<String>();
        for (ItemDraft item : items) {
            if (item == null || item.key() == null || item.key().isBlank() || item.key().length() > 64
                    || item.text() == null || item.text().isBlank() || item.text().length() > 255
                    || !keys.add(item.key()) || !labels.add(item.text().strip())) {
                throw new IllegalArgumentException("Each side requires unique, nonblank item keys and texts");
            }
        }
        return keys;
    }

    private void validateOrdering(OrderingDraft ordering) {
        if (ordering == null || ordering.items() == null || ordering.correctOrder() == null
                || ordering.items().size() < 2 || ordering.items().size() > 20) {
            throw new IllegalArgumentException("Ordering questions require 2–20 items and a correct order");
        }
        var keys = new HashSet<String>();
        var labels = new HashSet<String>();
        for (ItemDraft item : ordering.items()) {
            if (item == null || item.key() == null || item.key().isBlank() || item.key().length() > 64
                    || item.text() == null || item.text().isBlank() || item.text().length() > 255
                    || !keys.add(item.key()) || !labels.add(item.text().strip())) {
                throw new IllegalArgumentException("Ordering item keys and display texts must be nonblank and unique");
            }
        }
        if (ordering.correctOrder().size() != keys.size()
                || !new HashSet<>(ordering.correctOrder()).equals(keys)) {
            throw new IllegalArgumentException("Correct order must contain every item key exactly once");
        }
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

    @Transactional
    public Quiz updateTitle(Long quizId, String title) {
        Content content = contentRepository.findById(quizId)
            .orElseThrow(() -> new NoSuchElementException("Quiz not found"));
        if (!(Hibernate.unproxy(content) instanceof Quiz quiz)) {
            throw new NoSuchElementException("Quiz not found");
        }
        quiz.changeTitle(title);
        return quiz;
    }

    @Transactional(readOnly = true)
    public Long courseId(Long quizId) {
        Content content = contentRepository.findById(quizId)
            .orElseThrow(() -> new NoSuchElementException("Quiz not found"));
        if (!(Hibernate.unproxy(content) instanceof Quiz quiz)) {
            throw new NoSuchElementException("Quiz not found");
        }
        return quiz.getModule().getCourse().getId();
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

        if (!(Hibernate.unproxy(content) instanceof Quiz quiz)) {
            throw new NoSuchElementException(
                "Quiz not found: " + quizId
            );
        }

        List<QuestionWithOptions> questions =
            questionRepository
                .findByQuiz_IdOrderByPositionAsc(quizId)
                .stream()
                .map(question -> {
                    // DTO conversion happens after this transaction, including when OSIV is disabled.
                    if (question.getType() == QuestionType.ORDERING || question.getType() == QuestionType.MATCHING) {
                        question.getItems();
                    }
                    return new QuestionWithOptions(question,
                        optionRepository.findByQuestion_IdOrderByPositionAsc(question.getId()));
                })
                .toList();

        return new QuizWithQuestions(quiz, questions);
    }
}
