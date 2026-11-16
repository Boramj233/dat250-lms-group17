package no.hvl.dat250.lms.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Immutable metadata for one submission version; binary data lives in object storage. */
@Embeddable
public class SubmissionFileMetadata {
    @Column(name = "file_bucket", length = 63)
    private String bucket;

    @Column(name = "file_object_key", length = 1024)
    private String objectKey;

    @Column(name = "file_original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_media_type", length = 127)
    private String mediaType;

    @Column(name = "file_size_bytes")
    private Long sizeBytes;

    @Column(name = "file_sha256", length = 64)
    private String sha256;

    protected SubmissionFileMetadata() {
        // Required by JPA. All columns stay nullable for legacy TEXT/GIT_URL rows.
    }

    public SubmissionFileMetadata(String bucket, String objectKey, String originalFilename,
                                  String mediaType, long sizeBytes, String sha256) {
        requireText(bucket, 63, "Bucket");
        requireText(objectKey, 1024, "Object key");
        requireText(originalFilename, 255, "Filename");
        requireText(mediaType, 127, "Media type");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("SHA-256 must be 64 lowercase hexadecimal characters");
        }
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.mediaType = mediaType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    private static void requireText(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(field + " is required and must fit its storage limit");
        }
    }

    public String getBucket() { return bucket; }
    public String getObjectKey() { return objectKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getMediaType() { return mediaType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
}
