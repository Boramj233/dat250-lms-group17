package no.hvl.dat250.lms.submission.service;

/** A submission rejected by the configured deadline. */
public class SubmissionDeadlinePassedException extends IllegalStateException {
    public SubmissionDeadlinePassedException() {
        super("Submission deadline has passed");
    }
}
