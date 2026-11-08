
package no.hvl.dat250.lms.content.domain;

import java.time.Instant;

import jakarta.persistence.*;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    protected QuizAttempt() {}

    public QuizAttempt(
            Quiz quiz,
            Enrollment enrollment,
            Instant submittedAt,
            int correctCount,
            int totalQuestions) {

        if (quiz == null || enrollment == null) {
            throw new IllegalArgumentException(
                "Quiz and Enrollment are required");
        }

        if (submittedAt == null) {
            throw new IllegalArgumentException(
                "Submission time is required");
        }

        if (totalQuestions < 1 ||
                correctCount < 0 ||
                correctCount > totalQuestions) {
            throw new IllegalArgumentException(
                "Invalid quiz score");
        }

        this.quiz = quiz;
        this.enrollment = enrollment;
        this.submittedAt = submittedAt;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public Enrollment getEnrollment() { return enrollment; }
    public Instant getSubmittedAt() { return submittedAt; }
    public int getCorrectCount() { return correctCount; }
    public int getTotalQuestions() { return totalQuestions; }
}
