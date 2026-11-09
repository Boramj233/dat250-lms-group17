
package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

import no.hvl.dat250.lms.content.domain.QuizAttempt;

public record QuizAttemptResponse(

        Long id,
        Long quizId,
        Instant submittedAt,
        int correctCount,
        int totalQuestions

) {

    public static QuizAttemptResponse from(
            QuizAttempt attempt,
            Long quizId) {

        return new QuizAttemptResponse(
            attempt.getId(),
            quizId,
            attempt.getSubmittedAt(),
            attempt.getCorrectCount(),
            attempt.getTotalQuestions()
        );
    }
}
