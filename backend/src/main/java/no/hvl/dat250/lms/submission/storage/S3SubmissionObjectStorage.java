package no.hvl.dat250.lms.submission.storage;

import java.io.InputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

public final class S3SubmissionObjectStorage implements SubmissionObjectStorage {
    private final S3Client client;
    private final String bucket;

    public S3SubmissionObjectStorage(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public ObjectReference put(String key, InputStream content, long sizeBytes, String mediaType) {
        if (key == null || key.isBlank() || sizeBytes < 0 || content == null
                || mediaType == null || mediaType.isBlank()) {
            throw new IllegalArgumentException("Object key, content, size and media type are required");
        }
        try {
            client.putObject(r -> r.bucket(bucket).key(key).contentType(mediaType),
                    RequestBody.fromInputStream(content, sizeBytes));
            return new ObjectReference(bucket, key);
        } catch (SdkException ex) {
            throw new SubmissionStorageException("upload", ex);
        }
    }

    @Override
    public InputStream get(ObjectReference reference) {
        validate(reference);
        try {
            return client.getObject(r -> r.bucket(bucket).key(reference.key()));
        } catch (SdkException ex) {
            throw new SubmissionStorageException("download", ex);
        }
    }

    @Override
    public void delete(ObjectReference reference) {
        validate(reference);
        try {
            client.deleteObject(r -> r.bucket(bucket).key(reference.key()));
        } catch (SdkException ex) {
            throw new SubmissionStorageException("delete", ex);
        }
    }

    private void validate(ObjectReference reference) {
        if (reference == null || !bucket.equals(reference.bucket())
                || reference.key() == null || reference.key().isBlank()) {
            throw new IllegalArgumentException("Object must belong to the configured submission bucket");
        }
    }
}
