package no.hvl.dat250.lms.user.service;

/** A local profile already exists for the external email. */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() { super("A profile already exists for this email"); }
}
