package no.hvl.dat250.lms.user.api.dto;

import java.util.List;

/**
 * Report of a user import: what happened to every row, so the admin can fix and re-upload
 * the rows that failed. Re-uploading the same file is safe: existing emails are skipped.
 *
 * @param created rows that created a new profile
 * @param skipped rows whose email already has a profile (left unchanged)
 * @param failed rows that were invalid or repeated an email from an earlier row
 */
public record UserImportResponse(int created, int skipped, int failed, List<Row> rows) {

    public enum Status { CREATED, ALREADY_EXISTS, DUPLICATE_IN_FILE, INVALID }

    /**
     * @param userId the new or existing profile's ID; null for failed rows
     * @param errors why the row was rejected; empty unless INVALID
     */
    public record Row(int line, String email, Status status, Long userId, List<String> errors) {}
}
