package no.hvl.dat250.lms.content.api.dto;

import no.hvl.dat250.lms.content.domain.Quiz;

public record QuizResponse(
    Long id,
    Long courseId,
    Long moduleId,
    String title,
    int position,
    int maxAttempts
) {
    public static QuizResponse from(
            Quiz quiz,
            Long courseId,
            Long moduleId) {

        return new QuizResponse(
            quiz.getId(),
            courseId,
            moduleId,
            quiz.getTitle(),
            quiz.getPosition(),
            quiz.getMaxAttempts()
        );
    }
}