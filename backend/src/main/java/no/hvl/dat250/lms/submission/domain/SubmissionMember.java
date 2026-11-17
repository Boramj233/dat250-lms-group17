package no.hvl.dat250.lms.submission.domain;

import jakarta.persistence.*;

/** Immutable identity snapshot; independent of current Group membership. */
@Embeddable
public class SubmissionMember {
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;
    protected SubmissionMember() {}
    public SubmissionMember(Long userId, Long enrollmentId) {
        this.userId = userId;
        this.enrollmentId = enrollmentId;
    }
    public Long getUserId() { return userId; }
    public Long getEnrollmentId() { return enrollmentId; }
}
