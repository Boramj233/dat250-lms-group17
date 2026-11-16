package no.hvl.dat250.lms.submission.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionFileValidatorTest {

    private final SubmissionFileValidator validator = new SubmissionFileValidator(DataSize.ofMegabytes(10));

    @Test
    void validatesPdfHashesBytesAndRemovesPathAndControlCharacters() throws Exception {
        byte[] bytes = "%PDF-1.7\nReport\n".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "C:\\fakepath\\report\r\n.pdf", "application/pdf", bytes);

        var result = validator.validate(file);

        assertEquals("report.pdf", result.filename());
        assertEquals("application/pdf", result.mediaType());
        assertEquals(bytes.length, result.sizeBytes());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), result.sha256());
    }

    @Test
    void requiresExtensionMimeAndHeaderRatherThanTrustingOneOfThem() {
        assertThrows(UnsupportedSubmissionFileException.class, () -> validator.validate(
                new MockMultipartFile("file", "report.docx", "application/pdf", "%PDF-1.7".getBytes())));
        assertThrows(UnsupportedSubmissionFileException.class, () -> validator.validate(
                new MockMultipartFile("file", "report.pdf", "application/zip", "%PDF-1.7".getBytes())));
        assertThrows(UnsupportedSubmissionFileException.class, () -> validator.validate(
                new MockMultipartFile("file", "report.pdf", "application/pdf", "fake pdf".getBytes())));
    }

    @Test
    void acceptsExactlyTenMiBAndRejectsOneByteMore() throws Exception {
        int limit = 10 * 1024 * 1024;
        byte[] bytes = new byte[limit];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 5);

        assertEquals(limit, validator.validate(new MockMultipartFile(
                "file", "report.pdf", "application/pdf", bytes)).sizeBytes());
        assertThrows(MaxUploadSizeExceededException.class, () -> validator.validate(new MockMultipartFile(
                "file", "report.pdf", "application/pdf", java.util.Arrays.copyOf(bytes, limit + 1))));
    }

    @Test
    void countsActualBytesEvenWhenReportedSizeIsFalse() {
        var smallLimitValidator = new SubmissionFileValidator(DataSize.ofBytes(8));
        var file = new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-123456789".getBytes()) {
            @Override
            public long getSize() {
                return 5;
            }
        };

        assertThrows(MaxUploadSizeExceededException.class, () -> smallLimitValidator.validate(file));
    }

    @Test
    void rejectsEmptyFileAndMissingOrOversizedFilename() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "", "application/pdf", "%PDF-1.7".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "a".repeat(256) + ".pdf", "application/pdf", "%PDF-1.7".getBytes())));
    }
}
