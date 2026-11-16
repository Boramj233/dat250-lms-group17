
package no.hvl.dat250.lms.content.api.dto;

import no.hvl.dat250.lms.content.domain.QuestionType;

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
    List<QuestionResponse> questions
) {

    public record QuestionResponse(
        Long id,
        String questionText,
        int position,
        QuestionType type,
        List<OptionResponse> options,
        List<ItemResponse> items
    ) {}

    public record ItemResponse(UUID id, String text) {
    }

    public record OptionResponse(
        Long id,
        String optionText,
        int position
    ) {}

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
                    item.question().getType() != QuestionType.ORDERING ? List.of() : item.question().getItems().stream()
                        .map(value -> new ItemResponse(value.getId(), value.getText()))
                        .sorted(Comparator.comparing(value -> value.id().toString()))
                        .toList()
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
            questions
        );
    }
}
