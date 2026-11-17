package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;
import java.util.List;

import no.hvl.dat250.lms.content.domain.QuizAttempt;
import no.hvl.dat250.lms.content.service.QuizAttemptService.AttemptResult;

public record QuizAttemptResponse(
        Long id,
        Long quizId,
        Instant submittedAt,
        int correctCount,
        int totalQuestions,
        List<HintResponse> hints) {

    public record HintResponse(Long questionId, String hint) {
    }

    public static QuizAttemptResponse from(QuizAttempt attempt, Long quizId) {
        return new QuizAttemptResponse(attempt.getId(), quizId, attempt.getSubmittedAt(),
            attempt.getCorrectCount(), attempt.getTotalQuestions(), List.of());
    }

    public static QuizAttemptResponse from(AttemptResult result, Long quizId) {
        QuizAttempt attempt = result.attempt();
        List<HintResponse> hints = result.hints().stream()
            .map(hint -> new HintResponse(hint.questionId(), hint.hint()))
            .toList();
        return new QuizAttemptResponse(attempt.getId(), quizId, attempt.getSubmittedAt(),
            attempt.getCorrectCount(), attempt.getTotalQuestions(), hints);
    }
}
