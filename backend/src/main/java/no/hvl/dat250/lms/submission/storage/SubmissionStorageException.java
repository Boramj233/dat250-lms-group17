package no.hvl.dat250.lms.submission.storage;

public class SubmissionStorageException extends RuntimeException {
    public SubmissionStorageException(String operation, Throwable cause) {
        super("Submission storage " + operation + " failed", cause);
    }
}
