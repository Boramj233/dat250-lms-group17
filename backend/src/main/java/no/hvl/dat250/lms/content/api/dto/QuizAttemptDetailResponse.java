package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import no.hvl.dat250.lms.content.domain.QuizAnswer;
import no.hvl.dat250.lms.content.domain.QuizAttempt;
import no.hvl.dat250.lms.content.api.dto.QuizAttemptResponse.HintResponse;
import no.hvl.dat250.lms.content.api.dto.QuizDetailResponse.QuestionResponse;

/** Own submitted answers and safe question display data; never a standard answer key. */
public record QuizAttemptDetailResponse(
    Long id,
    Long quizId,
    Instant submittedAt,
    int correctCount,
    int totalQuestions,
    List<HintResponse> hints,
    List<AnswerResponse> answers
) {
    public record MatchResponse(UUID leftItemId, UUID rightItemId) {
    }

    public record AnswerResponse(
        Long questionId,
        QuestionResponse question,
        List<Long> selectedOptionIds,
        String textAnswer,
        List<UUID> orderedItemIds,
        List<MatchResponse> matches,
        boolean correct
    ) {
    }

    /** Called within the read transaction so lazy associations never escape to JSON serialization. */
    public static QuizAttemptDetailResponse from(QuizAttempt attempt, QuizDetailResponse quiz,
                                                 List<QuizAnswer> storedAnswers) {
        Map<Long, QuestionResponse> questions = quiz.questions().stream()
            .collect(Collectors.toMap(QuestionResponse::id, question -> question));
        List<AnswerResponse> answers = storedAnswers.stream()
            .map(answer -> new AnswerResponse(
                answer.getQuestion().getId(),
                questions.get(answer.getQuestion().getId()),
                answer.getSelectedOptions().stream().map(option -> option.getId()).sorted().toList(),
                answer.getTextAnswer(),
                answer.getOrderedItems().stream().map(item -> item.getId()).toList(),
                answer.getMatches().stream()
                    .map(pair -> new MatchResponse(pair.getLeftItem().getId(), pair.getRightItem().getId()))
                    .sorted(Comparator.comparing(pair -> pair.leftItemId().toString())).toList(),
                answer.isCorrect()))
            .toList();
        List<HintResponse> hints = storedAnswers.stream()
            .filter(answer -> !answer.isCorrect())
            .filter(answer -> answer.getQuestion().getHint() != null && !answer.getQuestion().getHint().isBlank())
            .map(answer -> new HintResponse(answer.getQuestion().getId(), answer.getQuestion().getHint()))
            .toList();
        return new QuizAttemptDetailResponse(attempt.getId(), quiz.id(), attempt.getSubmittedAt(),
            attempt.getCorrectCount(), attempt.getTotalQuestions(), hints, answers);
    }
}
