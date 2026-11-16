package no.hvl.dat250.lms.submission.storage;

import java.io.InputStream;

/** Private binary storage only; the calling service must authorize access first. */
public interface SubmissionObjectStorage {
    record ObjectReference(String bucket, String key) {}

    ObjectReference put(String key, InputStream content, long sizeBytes, String mediaType);

    /** The caller owns and must close the returned stream. */
    InputStream get(ObjectReference reference);

    /** Idempotent compensation for an object whose SQL transaction rolled back. */
    void delete(ObjectReference reference);
}
