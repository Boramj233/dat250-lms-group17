
package no.hvl.dat250.lms.content.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(
    name = "quiz_answers",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"attempt_id", "question_id"}
    )
)
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "quiz_answer_selected_options",
        joinColumns = @JoinColumn(name = "answer_id"),
        inverseJoinColumns = @JoinColumn(name = "option_id"),
        uniqueConstraints = @UniqueConstraint(
            columnNames = {"answer_id", "option_id"}
        )
    )
    private Set<ChoiceOption> selectedOptions = new HashSet<>();

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected QuizAnswer() {}

    public QuizAnswer(
            QuizAttempt attempt,
            QuizQuestion question,
            Set<ChoiceOption> selectedOptions,
            boolean correct) {

        if (attempt == null || question == null) {
            throw new IllegalArgumentException(
                "Attempt and Question are required");
        }

        if (selectedOptions == null ||
                selectedOptions.stream().anyMatch(option -> option == null)) {
            throw new IllegalArgumentException(
                "Selected options must not contain null");
        }

        this.attempt = attempt;
        this.question = question;
        this.selectedOptions.addAll(selectedOptions);
        this.correct = correct;
    }

    public Long getId() { return id; }
    public QuizAttempt getAttempt() { return attempt; }
    public QuizQuestion getQuestion() { return question; }

    public Set<ChoiceOption> getSelectedOptions() {
        return Set.copyOf(selectedOptions);
    }

    public boolean isCorrect() { return correct; }
}
