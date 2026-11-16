
package no.hvl.dat250.lms.content.api.dto;

import java.util.List;

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
        List<OptionResponse> options
    ) {}

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
                    item.options().stream()
                        .map(option -> new OptionResponse(
                            option.getId(),
                            option.getOptionText(),
                            option.getPosition()
                        ))
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
