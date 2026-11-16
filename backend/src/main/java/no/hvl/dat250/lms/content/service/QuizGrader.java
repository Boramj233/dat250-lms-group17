
package no.hvl.dat250.lms.content.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.hvl.dat250.lms.content.domain.ChoiceOption;

public class QuizGrader {

    public boolean isOrderCorrect(List<UUID> correctOrder, List<UUID> submittedOrder) {
        if (correctOrder == null || correctOrder.size() < 2 || correctOrder.size() > 20
                || correctOrder.stream().anyMatch(id -> id == null)
                || new HashSet<>(correctOrder).size() != correctOrder.size()) {
            throw new IllegalStateException("Saved ordering question must contain 2–20 distinct items");
        }
        if (submittedOrder == null || submittedOrder.size() != correctOrder.size()
                || !new HashSet<>(submittedOrder).equals(new HashSet<>(correctOrder))) {
            throw new IllegalArgumentException("Submitted order must contain every question item exactly once");
        }
        return submittedOrder.equals(correctOrder);
    }

    public boolean isTextCorrect(Set<String> acceptedAnswers, String textAnswer) {
        if (acceptedAnswers == null || acceptedAnswers.isEmpty()) {
            throw new IllegalStateException("Fill-in question has no accepted answers");
        }
        if (textAnswer == null || textAnswer.length() > 255) {
            throw new IllegalArgumentException("Text answer is required and must not exceed 255 characters");
        }
        if (textAnswer.isBlank()) {
            return false;
        }
        String normalized = textAnswer.strip();
        return acceptedAnswers.stream().anyMatch(answer -> answer.strip().equalsIgnoreCase(normalized));
    }

    public boolean isCorrect(
            List<ChoiceOption> options,
            List<Long> selectedOptionIds) {

        if (options == null || options.size() < 2) {
            throw new IllegalStateException(
                "Question must have at least two saved options");
        }

        if (selectedOptionIds == null) {
            throw new IllegalArgumentException(
                "Selected options must not be null");
        }

        Set<Long> validIds = new HashSet<>();
        Set<Long> correctIds = new HashSet<>();

        // Build the answer key from stored database options.
        for (ChoiceOption option : options) {
            if (option == null || option.getId() == null
                    || !validIds.add(option.getId())) {
                throw new IllegalStateException(
                    "Invalid saved question options");
            }

            if (option.isCorrect()) {
                correctIds.add(option.getId());
            }
        }

        if (correctIds.isEmpty()) {
            throw new IllegalStateException(
                "Question has no correct option");
        }

        Set<Long> selectedIds = new HashSet<>();

        // Validate all IDs submitted by the student.
        for (Long id : selectedOptionIds) {
            if (id == null || !validIds.contains(id)
                    || !selectedIds.add(id)) {
                throw new IllegalArgumentException(
                    "Invalid or duplicate selected option ID");
            }
        }

        // Exact-set matching: order does not matter.
        return selectedIds.equals(correctIds);
    }
}
