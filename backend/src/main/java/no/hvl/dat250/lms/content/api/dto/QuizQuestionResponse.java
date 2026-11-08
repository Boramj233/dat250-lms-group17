package no.hvl.dat250.lms.content.api.dto;

import no.hvl.dat250.lms.content.domain.QuizQuestion;

public record QuizQuestionResponse(
    Long id,
    Long quizId,
    String questionText,
    int position,
    String hint
) {
    public static QuizQuestionResponse from(
            QuizQuestion question,
            Long quizId) {

        return new QuizQuestionResponse(
            question.getId(),
            quizId,
            question.getQuestionText(),
            question.getPosition(),
            question.getHint()
        );
    }
}