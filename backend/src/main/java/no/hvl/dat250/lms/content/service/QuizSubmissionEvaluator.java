package no.hvl.dat250.lms.content.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.hvl.dat250.lms.content.service.QuizGrader.MatchIds;

import no.hvl.dat250.lms.content.domain.QuestionType;
import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;

public class QuizSubmissionEvaluator {

    private final QuizGrader grader = new QuizGrader();

    public record AnswerInput(Long questionId, List<Long> selectedOptionIds, String textAnswer,
                              List<UUID> orderedItemIds, List<MatchIds> matches) {
        public AnswerInput(Long questionId, List<Long> selectedOptionIds, String textAnswer, List<UUID> orderedItemIds) {
            this(questionId, selectedOptionIds, textAnswer, orderedItemIds, null);
        }
        public AnswerInput(Long questionId, List<Long> selectedOptionIds, String textAnswer) {
            this(questionId, selectedOptionIds, textAnswer, null, null);
        }
        public AnswerInput(Long questionId, List<Long> selectedOptionIds) {
            this(questionId, selectedOptionIds, null, null, null);
        }
    }

    public record GradedAnswer(
            Long questionId, List<Long> selectedOptionIds, boolean correct, String textAnswer,
            List<UUID> orderedItemIds, List<MatchIds> matches) {
    }

    public record Result(int correctCount, int totalQuestions, List<GradedAnswer> answers) {
    }

    public Result evaluate(QuizWithQuestions quizDetails, List<AnswerInput> submittedAnswers) {
        if (quizDetails == null || quizDetails.questions() == null || quizDetails.questions().isEmpty()) {
            throw new IllegalStateException("Quiz must contain questions");
        }
        if (submittedAnswers == null) {
            throw new IllegalArgumentException("Answers are required");
        }

        Map<Long, AnswerInput> answersByQuestion = new HashMap<>();
        for (AnswerInput answer : submittedAnswers) {
            if (answer == null || answer.questionId() == null) {
                throw new IllegalArgumentException("Invalid answer");
            }
            if (answersByQuestion.putIfAbsent(answer.questionId(), answer) != null) {
                throw new IllegalArgumentException("Duplicate question answer");
            }
        }
        if (answersByQuestion.size() != quizDetails.questions().size()) {
            throw new IllegalArgumentException("Incorrect number of question answers");
        }

        List<GradedAnswer> gradedAnswers = new ArrayList<>();
        int correctCount = 0;
        for (var item : quizDetails.questions()) {
            Long questionId = item.question().getId();
            AnswerInput answer = answersByQuestion.get(questionId);
            if (answer == null) {
                throw new IllegalArgumentException("Missing or unknown question ID");
            }

            boolean correct;
            List<Long> selected = List.of();
            List<UUID> ordered = List.of();
            List<MatchIds> matches = List.of();
            QuestionType type = item.question().getType();
            if (type != QuestionType.ORDERING && answer.orderedItemIds() != null
                    && !answer.orderedItemIds().isEmpty()) {
                throw new IllegalArgumentException("Only ordering answers may contain ordered items");
            }
            if (type != QuestionType.MATCHING && answer.matches() != null && !answer.matches().isEmpty()) {
                throw new IllegalArgumentException("Only matching answers may contain matches");
            }
            switch (type) {
                case MATCHING -> {
                    if (answer.textAnswer() != null || (answer.selectedOptionIds() != null
                            && !answer.selectedOptionIds().isEmpty())) {
                        throw new IllegalArgumentException("Matching answers must not contain text or selected options");
                    }
                    List<MatchIds> expected = item.question().getCorrectMatches().stream()
                        .map(pair -> new MatchIds(pair.getLeftItem().getId(), pair.getRightItem().getId())).toList();
                    correct = grader.isMatchingCorrect(expected, answer.matches());
                    matches = List.copyOf(answer.matches());
                }
                case ORDERING -> {
                    if (answer.textAnswer() != null || (answer.selectedOptionIds() != null
                            && !answer.selectedOptionIds().isEmpty())) {
                        throw new IllegalArgumentException("Ordering answers must not contain text or selected options");
                    }
                    List<UUID> expected = item.question().getCorrectOrder().stream()
                        .map(value -> value.getId()).toList();
                    correct = grader.isOrderCorrect(expected, answer.orderedItemIds());
                    ordered = List.copyOf(answer.orderedItemIds());
                }
                case FILL_IN_BLANK -> {
                    if (answer.selectedOptionIds() != null && !answer.selectedOptionIds().isEmpty()) {
                        throw new IllegalArgumentException("Fill-in answers must not contain selected options");
                    }
                    correct = grader.isTextCorrect(item.question().getAcceptedAnswers(), answer.textAnswer());
                }
                case MULTIPLE_CHOICE, TRUE_FALSE -> {
                    if (answer.textAnswer() != null) {
                        throw new IllegalArgumentException("Choice answers must not contain text");
                    }
                    selected = answer.selectedOptionIds();
                    if (type == QuestionType.TRUE_FALSE && (selected == null || selected.size() != 1)) {
                        throw new IllegalArgumentException("True/false answers require exactly one option");
                    }
                    correct = grader.isCorrect(item.options(), selected);
                }
                default -> throw new IllegalStateException("Unsupported question type");
            }
            if (correct) {
                correctCount++;
            }
            gradedAnswers.add(new GradedAnswer(questionId, List.copyOf(selected), correct, answer.textAnswer(), ordered, matches));
        }
        return new Result(correctCount, gradedAnswers.size(), List.copyOf(gradedAnswers));
    }
}
