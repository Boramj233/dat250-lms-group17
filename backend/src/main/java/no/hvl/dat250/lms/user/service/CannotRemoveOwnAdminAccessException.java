package no.hvl.dat250.lms.user.service;

/** An admin tried to remove their own ADMIN access (answered with 403). */
public class CannotRemoveOwnAdminAccessException extends RuntimeException {
    public CannotRemoveOwnAdminAccessException() {
        super("Cannot remove your own administrator access");
    }
}
