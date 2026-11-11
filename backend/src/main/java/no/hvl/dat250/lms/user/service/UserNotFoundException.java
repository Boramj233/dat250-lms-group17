package no.hvl.dat250.lms.user.service;

/** The requested user no longer exists. */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("User not found");
    }
}
