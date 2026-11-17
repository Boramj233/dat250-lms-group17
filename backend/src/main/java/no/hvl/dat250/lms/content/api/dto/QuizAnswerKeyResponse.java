package no.hvl.dat250.lms.content.api.dto;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import no.hvl.dat250.lms.content.api.dto.QuizAttemptDetailResponse.MatchResponse;
import no.hvl.dat250.lms.content.api.dto.QuizDetailResponse.QuestionResponse;
import no.hvl.dat250.lms.content.domain.ChoiceOption;
import no.hvl.dat250.lms.content.domain.QuizItem;
import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;

/** Teacher-only answer keys, kept separate from every student response. */
public record QuizAnswerKeyResponse(Long quizId, List<QuestionKeyResponse> questions) {
    public record QuestionKeyResponse(
        QuestionResponse question,
        String hint,
        List<Long> correctOptionIds,
        List<String> acceptedAnswers,
        List<UUID> correctOrder,
        List<MatchResponse> correctMatches
    ) {
    }

    /** Assemble inside the read transaction, including lazy answer-key collections. */
    public static QuizAnswerKeyResponse from(QuizWithQuestions details) {
        var displayQuestions = QuizDetailResponse.from(details).questions().stream()
            .collect(Collectors.toMap(QuestionResponse::id, question -> question));
        List<QuestionKeyResponse> questions = details.questions().stream().map(item -> {
            var question = item.question();
            return new QuestionKeyResponse(
                displayQuestions.get(question.getId()),
                question.getHint(),
                item.options().stream().filter(ChoiceOption::isCorrect).map(ChoiceOption::getId).sorted().toList(),
                question.getAcceptedAnswers().stream().sorted().toList(),
                question.getCorrectOrder().stream().map(QuizItem::getId).toList(),
                question.getCorrectMatches().stream()
                    .map(pair -> new MatchResponse(pair.getLeftItem().getId(), pair.getRightItem().getId()))
                    .sorted(Comparator.comparing(pair -> pair.leftItemId().toString())).toList());
        }).toList();
        return new QuizAnswerKeyResponse(details.quiz().getId(), questions);
    }
}
