package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "question_position", nullable = false)
    private int position;

    private String hint;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 32)
    private QuestionType type = QuestionType.MULTIPLE_CHOICE;

    @ElementCollection
    @CollectionTable(name = "quiz_accepted_answers", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "answer_text", nullable = false, length = 255)
    private Set<String> acceptedAnswers = new HashSet<>();

    public QuestionType getType() { return type; }

    public Set<String> getAcceptedAnswers() {
        return Set.copyOf(acceptedAnswers);
    }

    public QuizQuestion(Quiz quiz, String questionText, int position, String hint,
                        QuestionType type, List<String> acceptedAnswers) {
        this(quiz, questionText, position, hint);
        if (type == null) {
            throw new IllegalArgumentException("Question type is required");
        }
        if (acceptedAnswers == null) {
            acceptedAnswers = List.of();
        }
        if (type == QuestionType.FILL_IN_BLANK) {
            if (acceptedAnswers.isEmpty() || acceptedAnswers.size() > 20
                    || acceptedAnswers.stream().anyMatch(answer -> answer == null
                        || answer.isBlank() || answer.length() > 255)) {
                throw new IllegalArgumentException("Fill-in questions need 1–20 nonblank accepted answers of at most 255 characters");
            }
        } else if (!acceptedAnswers.isEmpty()) {
            throw new IllegalArgumentException("Only fill-in questions accept text answer keys");
        }
        this.type = type;
        this.acceptedAnswers.addAll(acceptedAnswers);
    }

    protected QuizQuestion() {}

    public QuizQuestion(
            Quiz quiz,
            String questionText,
            int position,
            String hint) {

        if (quiz == null) {
            throw new IllegalArgumentException("Quiz is required");
        }

        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException(
                "Question text must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Question position must not be negative");
        }

        this.quiz = quiz;
        this.questionText = questionText;
        this.position = position;
        this.hint = hint;
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getQuestionText() { return questionText; }
    public int getPosition() { return position; }
    public String getHint() { return hint; }
}