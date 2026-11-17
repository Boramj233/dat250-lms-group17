package no.hvl.dat250.lms.grade.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.submission.domain.Submission;

@Entity
@Table(name = "grades", uniqueConstraints = @UniqueConstraint(columnNames = "submission_id"))
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    // Drafts may remain undecided; release requires an explicit Pass/Fail decision.
    @Column(name = "passed")
    private Boolean passed;

    public Boolean getPassed() { return passed; }

    public void decidePassed(Boolean passed) {
        if (releasedAt != null) throw new IllegalStateException("Released grades cannot be edited");
        this.passed = passed;
    }

    @Column(length = 10000)
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "graded_by_id", nullable = false)
    private Enrollment gradedBy;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by_id")
    private Enrollment releasedBy;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Grade() {}

    public Grade(Submission submission, BigDecimal score, String feedback,
                 Enrollment teacher, Instant now) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required");
        }
        this.submission = submission;
        revise(score, feedback, teacher, now);
    }

    public void revise(BigDecimal score, String feedback, Enrollment teacher, Instant now) {
        if (releasedAt != null) {
            throw new IllegalStateException("Released grades cannot be edited");
        }
        if (score == null || score.signum() < 0 || score.compareTo(new BigDecimal("100")) > 0
                || score.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Score must be between 0 and 100 with at most two decimal places");
        }
        if (feedback != null && feedback.length() > 10000) {
            throw new IllegalArgumentException("Feedback must not exceed 10000 characters");
        }
        if (teacher == null || now == null) {
            throw new IllegalArgumentException("Teacher and timestamp are required");
        }
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = teacher;
        this.gradedAt = now;
    }

    public void release(Enrollment teacher, Instant now) {
        if (teacher == null || now == null) {
            throw new IllegalArgumentException("Teacher and timestamp are required");
        }
        if (releasedAt == null) {
            if (passed == null) {
                throw new IllegalArgumentException("Choose Pass or Fail before releasing the grade");
            }
            releasedBy = teacher;
            releasedAt = now;
        }
    }

    public Long getId() { return id; }
    public Submission getSubmission() { return submission; }
    public BigDecimal getScore() { return score; }
    public String getFeedback() { return feedback; }
    public Enrollment getGradedBy() { return gradedBy; }
    public Instant getGradedAt() { return gradedAt; }
    public Enrollment getReleasedBy() { return releasedBy; }
    public Instant getReleasedAt() { return releasedAt; }
    public Long getVersion() { return version; }
}
