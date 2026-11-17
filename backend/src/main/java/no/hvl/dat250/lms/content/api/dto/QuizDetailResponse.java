
package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

import no.hvl.dat250.lms.content.domain.QuestionType;
import no.hvl.dat250.lms.content.domain.QuizQuestion;
import no.hvl.dat250.lms.content.domain.QuizItemKind;

import java.util.List;
import java.util.UUID;
import java.util.Comparator;

import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;

public record QuizDetailResponse(
    Long id,
    Long courseId,
    Long moduleId,
    String title,
    int position,
    int maxAttempts,
    int passingPercentage,
    List<QuestionResponse> questions,
    Instant visibleFrom,
    Instant visibleUntil
) {

    public record QuestionResponse(
        Long id,
        String questionText,
        int position,
        QuestionType type,
        List<OptionResponse> options,
        List<ItemResponse> items,
        List<ItemResponse> leftItems,
        List<ItemResponse> rightItems
    ) {}

    public record ItemResponse(UUID id, String text) {
    }

    public record OptionResponse(
        Long id,
        String optionText,
        int position
    ) {}

    private static List<ItemResponse> displayItems(QuizQuestion question, QuizItemKind kind) {
        if (question.getType() != QuestionType.ORDERING && question.getType() != QuestionType.MATCHING) {
            return List.of();
        }
        return question.getItems().stream()
            .filter(item -> item.getKind() == kind)
            .map(item -> new ItemResponse(item.getId(), item.getText()))
            .sorted(Comparator.comparing(item -> item.id().toString()))
            .toList();
    }

    public static QuizDetailResponse from(
            QuizWithQuestions details) {

        Quiz quiz = details.quiz();

        List<QuestionResponse> questions =
            details.questions().stream()
                .map(item -> new QuestionResponse(
                    item.question().getId(),
                    item.question().getQuestionText(),
                    item.question().getPosition(),
                    item.question().getType(),
                    item.options().stream()
                        .map(option -> new OptionResponse(
                            option.getId(),
                            option.getOptionText(),
                            option.getPosition()
                        ))
                        .toList(),
                    displayItems(item.question(), QuizItemKind.ORDERING),
                    displayItems(item.question(), QuizItemKind.MATCHING_LEFT),
                    displayItems(item.question(), QuizItemKind.MATCHING_RIGHT)
                ))
                .toList();

        return new QuizDetailResponse(
            quiz.getId(),
            quiz.getModule().getCourse().getId(),
            quiz.getModule().getId(),
            quiz.getTitle(),
            quiz.getPosition(),
            quiz.getMaxAttempts(),
            quiz.getPassingPercentage(),
            questions,
            quiz.getVisibleFrom(),
            quiz.getVisibleUntil()
        );
    }
}
