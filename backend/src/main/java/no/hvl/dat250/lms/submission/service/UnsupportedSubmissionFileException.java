package no.hvl.dat250.lms.submission.service;

public class UnsupportedSubmissionFileException extends IllegalArgumentException {

    public UnsupportedSubmissionFileException() {
        super("Only PDF files with application/pdf content type and a PDF header are accepted");
    }
}
