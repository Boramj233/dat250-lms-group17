package no.hvl.dat250.lms.user.service;

/**
 * One row of a user import, as read from the file and not yet validated.
 *
 * @param line the row's line number in the file (the header is line 1), for the report
 * @param access the access level's name, or blank for STANDARD
 */
public record UserImportRow(int line, String email, String firstName, String lastName, String access) {}
