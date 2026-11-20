package no.hvl.dat250.lms.group.domain;

public class GroupFullException extends IllegalStateException {

    public GroupFullException() {
        super("Group has reached maximum capacity");
    }
}
