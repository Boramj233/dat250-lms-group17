package no.hvl.dat250.lms.content.service;

public class ContentDeletionConflictException extends RuntimeException {
    public ContentDeletionConflictException(String message) {
        super(message);
    }
}
