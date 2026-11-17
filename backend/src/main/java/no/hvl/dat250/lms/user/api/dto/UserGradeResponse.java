package no.hvl.dat250.lms.user.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One assignment or coding exercise the user has handed in, with the grade of the
 * latest submission once it is released (GET /api/users/me/grades).
 *
 * @param type ASSIGNMENT or CODING_EXERCISE
 * @param submissionId the latest submission, which is the one that is graded
 * @param grade the released grade; null while there is none or it is not released
 */
public record UserGradeResponse(
        Long contentId,
        String type,
        String title,
        Long courseId,
        String courseTitle,
        Long moduleId,
        Long submissionId,
        Instant submittedAt,
        boolean late,
        ReleasedGrade grade) {

    /** The same fields as GET /api/submissions/{id}/grade, without the submission id. */
    public record ReleasedGrade(BigDecimal score, String feedback, Boolean passed, Instant releasedAt) {
    }
}
