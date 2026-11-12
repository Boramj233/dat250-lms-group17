package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "choice_options")
public class ChoiceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "option_position", nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean correct;

    protected ChoiceOption() {}

    public ChoiceOption(
            QuizQuestion question,
            String optionText,
            int position,
            boolean correct) {

        if (question == null) {
            throw new IllegalArgumentException(
                "Question is required");
        }

        if (optionText == null || optionText.isBlank()) {
            throw new IllegalArgumentException(
                "Option text must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Option position must not be negative");
        }

        this.question = question;
        this.optionText = optionText;
        this.position = position;
        this.correct = correct;
    }

    public Long getId() { return id; }
    public QuizQuestion getQuestion() { return question; }
    public String getOptionText() { return optionText; }
    public int getPosition() { return position; }
    public boolean isCorrect() { return correct; }
}