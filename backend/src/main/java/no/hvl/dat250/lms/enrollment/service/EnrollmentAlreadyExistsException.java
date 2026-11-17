package no.hvl.dat250.lms.enrollment.service;

/** The user already has an active enrollment in the course. */
public class EnrollmentAlreadyExistsException extends RuntimeException {
    public EnrollmentAlreadyExistsException() {
        super("The user is already enrolled in this course");
    }
}
