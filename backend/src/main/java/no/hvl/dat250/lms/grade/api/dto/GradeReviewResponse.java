package no.hvl.dat250.lms.grade.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import no.hvl.dat250.lms.grade.domain.Grade;

public record GradeReviewResponse(
        Long submissionId, BigDecimal score, String feedback,
        Long gradedByEnrollmentId, Instant gradedAt,
        Long releasedByEnrollmentId, Instant releasedAt, Long version) {
    public static GradeReviewResponse from(Grade grade) {
        Long releasedBy = null;
        if (grade.getReleasedBy() != null) {
            releasedBy = grade.getReleasedBy().getId();
        }
        return new GradeReviewResponse(grade.getSubmission().getId(), grade.getScore(), grade.getFeedback(),
            grade.getGradedBy().getId(), grade.getGradedAt(), releasedBy, grade.getReleasedAt(), grade.getVersion());
    }
}
