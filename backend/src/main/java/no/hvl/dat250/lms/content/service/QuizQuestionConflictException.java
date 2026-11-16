package no.hvl.dat250.lms.content.service;

public class QuizQuestionConflictException extends RuntimeException {
    public QuizQuestionConflictException() {
        super("Questions cannot be added after the first quiz attempt");
    }
}
