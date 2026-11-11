package no.hvl.dat250.lms.user.api;

/** The uploaded file cannot be read as a user list at all (answered with 400). */
public class InvalidUserCsvException extends RuntimeException {
    public InvalidUserCsvException(String message) {
        super(message);
    }
}
