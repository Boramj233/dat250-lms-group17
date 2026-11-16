package no.hvl.dat250.lms.content.domain;

import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

/** A question-owned display item. It contains no answer position. */
@Entity
@Table(name = "quiz_items")
public class QuizItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "item_text", nullable = false, length = 255)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 32)
    private QuizItemKind kind = QuizItemKind.ORDERING;

    public QuizItemKind getKind() { return kind; }

    protected QuizItem() {
    }

    public QuizItem(QuizQuestion question, String text) {
        this(question, text, QuizItemKind.ORDERING);
    }

    public QuizItem(QuizQuestion question, String text, QuizItemKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Item kind is required");
        }
        if (question == null || text == null || text.isBlank() || text.length() > 255) {
            throw new IllegalArgumentException("Question and nonblank item text of at most 255 characters are required");
        }
        this.question = question;
        this.text = text;
        this.kind = kind;
    }

    public UUID getId() { return id; }
    public QuizQuestion getQuestion() { return question; }
    public String getText() { return text; }
}
