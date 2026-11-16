package no.hvl.dat250.lms.grade.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import no.hvl.dat250.lms.grade.domain.Grade;

public record ReleasedGradeResponse(Long submissionId, BigDecimal score, String feedback, Instant releasedAt) {
    public static ReleasedGradeResponse from(Grade grade) {
        return new ReleasedGradeResponse(grade.getSubmission().getId(), grade.getScore(),
            grade.getFeedback(), grade.getReleasedAt());
    }
}
