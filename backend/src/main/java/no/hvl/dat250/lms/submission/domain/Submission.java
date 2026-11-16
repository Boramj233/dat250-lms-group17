package no.hvl.dat250.lms.submission.domain;

import java.time.Instant;

import jakarta.persistence.*;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private Enrollment submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false)
    private SubmissionType type;

    @Column(length = 10000)
    private String payload;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private boolean late;

    @Embedded
    private SubmissionFileMetadata file;

    protected Submission() {
        // Required by JPA
    }

    public Submission(
            Content content,
            Enrollment submittedBy,
            SubmissionType type,
            String payload,
            Instant submittedAt,
            boolean late) {

        this(content, submittedBy, type, payload, null, submittedAt, late);
    }

    public static Submission file(
            Assignment assignment,
            Enrollment submittedBy,
            SubmissionFileMetadata file,
            Instant submittedAt,
            boolean late) {
        return new Submission(assignment, submittedBy, SubmissionType.FILE, null, file, submittedAt, late);
    }

    private Submission(
            Content content,
            Enrollment submittedBy,
            SubmissionType type,
            String payload,
            SubmissionFileMetadata file,
            Instant submittedAt,
            boolean late) {

        if (!(content instanceof Assignment)
                && !(content instanceof CodingExercise)) {
            throw new IllegalArgumentException(
                "Submissions require an Assignment or CodingExercise"
            );
        }

        if (submittedBy == null) {
            throw new IllegalArgumentException(
                "Submitting enrollment is required"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                "Submission type is required"
            );
        }

        if (type == SubmissionType.FILE) {
            if (!(content instanceof Assignment) || file == null || payload != null) {
                throw new IllegalArgumentException("FILE requires an Assignment and file metadata without a payload");
            }
        } else if (file != null || payload == null || payload.isBlank()
                || payload.length() > 10000) {
            throw new IllegalArgumentException(
                "Valid submission payload is required"
            );
        }

        if (submittedAt == null) {
            throw new IllegalArgumentException(
                "Submission timestamp is required"
            );
        }

        this.content = content;
        this.submittedBy = submittedBy;
        this.type = type;
        this.payload = payload;
        this.file = file;
        this.submittedAt = submittedAt;
        this.late = late;
    }

    public Long getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public Enrollment getSubmittedBy() {
        return submittedBy;
    }

    public SubmissionType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public SubmissionFileMetadata getFile() {
        return file;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public boolean isLate() {
        return late;
    }
}
