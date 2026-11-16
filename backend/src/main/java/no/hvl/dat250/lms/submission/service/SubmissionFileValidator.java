package no.hvl.dat250.lms.submission.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@Component
public class SubmissionFileValidator {

    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final long maxSizeBytes;

    public record ValidatedFile(
            String filename,
            String mediaType,
            long sizeBytes,
            String sha256
    ) {}

    public SubmissionFileValidator(
            @Value("${lms.submission.files.max-size:10MB}") DataSize maxSize) {

        if (maxSize.toBytes() <= 0) {
            throw new IllegalArgumentException("Submission file size limit must be positive");
        }

        this.maxSizeBytes = maxSize.toBytes();
    }

    public ValidatedFile validate(
            MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty file is required");
        }

        requireWithinLimit(file.getSize());
        String filename = sanitizeFilename(file.getOriginalFilename());

        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new UnsupportedSubmissionFileException();
        }

        MessageDigest digest = sha256Digest();
        long sizeBytes;

        // Validate and hash with bounded memory. The multipart file can be reopened for upload.
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(PDF_HEADER.length);

            if (!Arrays.equals(header, PDF_HEADER)) {
                throw new UnsupportedSubmissionFileException();
            }

            digest.update(header);
            sizeBytes = header.length;
            requireWithinLimit(sizeBytes);

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                sizeBytes += bytesRead;
                requireWithinLimit(sizeBytes);
                digest.update(buffer, 0, bytesRead);
            }
        }

        if (sizeBytes != file.getSize()) {
            throw new IllegalArgumentException("File size does not match its contents");
        }

        return new ValidatedFile(
            filename,
            "application/pdf",
            sizeBytes,
            HexFormat.of().formatHex(digest.digest())
        );
    }

    private String sanitizeFilename(
            String originalFilename) {

        if (originalFilename == null) {
            throw new IllegalArgumentException("Filename is required");
        }

        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        filename = filename.replaceAll("[\\p{Cntrl}]", "").strip();

        if (filename.isBlank() || filename.length() > 255) {
            throw new IllegalArgumentException("Filename must contain between 1 and 255 characters");
        }

        return filename;
    }

    private void requireWithinLimit(
            long sizeBytes) {

        if (sizeBytes > maxSizeBytes) {
            throw new MaxUploadSizeExceededException(maxSizeBytes);
        }
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", ex);
        }
    }
}
