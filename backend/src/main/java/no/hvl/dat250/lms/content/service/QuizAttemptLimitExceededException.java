package no.hvl.dat250.lms.content.service;

/** A submission rejected by the configured attempt limit. */
public class QuizAttemptLimitExceededException extends IllegalStateException {
    public QuizAttemptLimitExceededException() {
        super("Maximum quiz attempts reached");
    }
}
