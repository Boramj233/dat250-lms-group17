
package no.hvl.dat250.lms.content.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;

public class QuizSubmissionEvaluator {

    private final QuizGrader grader = new QuizGrader();

    public record AnswerInput(
        Long questionId,
        List<Long> selectedOptionIds
    ) {}

    public record GradedAnswer(
        Long questionId,
        List<Long> selectedOptionIds,
        boolean correct
    ) {}

    public record Result(
        int correctCount,
        int totalQuestions,
        List<GradedAnswer> answers
    ) {}

    public Result evaluate(
            QuizWithQuestions quizDetails,
            List<AnswerInput> submittedAnswers) {

        if (quizDetails == null ||
                quizDetails.questions() == null ||
                quizDetails.questions().isEmpty()) {
            throw new IllegalStateException(
                "Quiz must contain questions");
        }

        if (submittedAnswers == null) {
            throw new IllegalArgumentException(
                "Answers are required");
        }

        // Map submitted answers by question ID.
        Map<Long, List<Long>> selections = new HashMap<>();

        for (AnswerInput answer : submittedAnswers) {

            if (answer == null ||
                    answer.questionId() == null ||
                    answer.selectedOptionIds() == null) {
                throw new IllegalArgumentException(
                    "Invalid answer");
            }

            if (selections.putIfAbsent(
                    answer.questionId(),
                    answer.selectedOptionIds()) != null) {
                throw new IllegalArgumentException(
                    "Duplicate question answer");
            }
        }

        // Each quiz question must appear exactly once.
        if (selections.size() != quizDetails.questions().size()) {
            throw new IllegalArgumentException(
                "Incorrect number of question answers");
        }

        List<GradedAnswer> gradedAnswers = new ArrayList<>();
        int correctCount = 0;

        for (var item : quizDetails.questions()) {

            Long questionId = item.question().getId();

            if (!selections.containsKey(questionId)) {
                throw new IllegalArgumentException(
                    "Missing or unknown question ID");
            }

            List<Long> selected = selections.get(questionId);

            boolean correct = grader.isCorrect(
                item.options(),
                selected
            );

            if (correct) {
                correctCount++;
            }

            gradedAnswers.add(new GradedAnswer(
                questionId,
                List.copyOf(selected),
                correct
            ));
        }

        return new Result(
            correctCount,
            gradedAnswers.size(),
            List.copyOf(gradedAnswers)
        );
    }
}
