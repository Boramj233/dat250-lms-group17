package no.hvl.dat250.lms.user.api.dto;

import java.time.Instant;

/**
 * One upcoming deadline in a course the user is enrolled in
 * (GET /api/users/{userId}/upcoming-deadlines).
 *
 * @param type ASSIGNMENT or CODING_EXERCISE
 * @param mandatoryForExam the assignment's flag; null for coding exercises
 */
public record UpcomingDeadlineResponse(
        Long contentId,
        String type,
        String title,
        Instant deadline,
        boolean allowLateSubmission,
        Boolean mandatoryForExam,
        Long courseId,
        String courseTitle,
        Long moduleId) {
}
