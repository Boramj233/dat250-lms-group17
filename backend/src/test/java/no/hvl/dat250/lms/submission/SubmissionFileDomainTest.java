package no.hvl.dat250.lms.submission;

import java.time.Instant;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.submission.api.dto.SubmissionResponse;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmissionFileDomainTest {
    private final Assignment assignment = mock(Assignment.class);
    private final Enrollment enrollment = mock(Enrollment.class, RETURNS_DEEP_STUBS);
    private final Instant now = Instant.parse("2026-11-16T12:00:00Z");

    private SubmissionFileMetadata metadata() {
        return new SubmissionFileMetadata("lms-submissions", "submissions/unique-key",
                "report.pdf", "application/pdf", 123, "a".repeat(64));
    }

    @Test
    void fileUsesMetadataWithoutTextPayload() {
        var metadata = metadata();
        var submission = Submission.file(assignment, enrollment, metadata, now, true);
        assertEquals(SubmissionType.FILE, submission.getType());
        assertNull(submission.getPayload());
        assertSame(metadata, submission.getFile());
        assertEquals(now, submission.getSubmittedAt());
        assertTrue(submission.isLate());
        var response = SubmissionResponse.from(submission);
        assertEquals("report.pdf", response.file().name());
        assertEquals("application/pdf", response.file().mediaType());
        assertEquals(123, response.file().sizeBytes());

    }

    @Test
    void ordinaryConstructorCannotForgeFileFromPayload() {
        assertThrows(IllegalArgumentException.class, () -> new Submission(
                assignment, enrollment, SubmissionType.FILE, "forged-object-key", now, false));
    }

    @Test
    void fileRequiresAssignmentEnrollmentMetadataAndTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> Submission.file(null, enrollment, metadata(), now, false));
        assertThrows(IllegalArgumentException.class, () -> Submission.file(assignment, null, metadata(), now, false));
        assertThrows(IllegalArgumentException.class, () -> Submission.file(assignment, enrollment, null, now, false));
        assertThrows(IllegalArgumentException.class, () -> Submission.file(assignment, enrollment, metadata(), null, false));
    }

    @Test
    void legacyChannelsKeepPayloadAndNoFile() {
        for (var type : new SubmissionType[] {SubmissionType.TEXT, SubmissionType.GIT_URL}) {
            var submission = new Submission(assignment, enrollment, type, "existing payload", now, false);
            assertEquals("existing payload", submission.getPayload());
            assertNull(submission.getFile());
            assertNull(SubmissionResponse.from(submission).file());
        }
    }

    @Test
    void metadataRejectsMissingOversizedOrInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new SubmissionFileMetadata(
                "", "key", "report.pdf", "application/pdf", 1, "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionFileMetadata(
                "bucket", " ", "report.pdf", "application/pdf", 1, "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionFileMetadata(
                "bucket", "key", "a".repeat(256), "application/pdf", 1, "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionFileMetadata(
                "bucket", "key", "report.pdf", null, 1, "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionFileMetadata(
                "bucket", "key", "report.pdf", "application/pdf", 0, "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionFileMetadata(
                "bucket", "key", "report.pdf", "application/pdf", 1, "invalid-hash"));
    }
}
