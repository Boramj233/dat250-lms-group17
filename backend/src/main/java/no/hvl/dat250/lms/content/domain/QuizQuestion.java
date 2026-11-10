package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.*;

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