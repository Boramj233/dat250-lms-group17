package no.hvl.dat250.lms.submission.service;

import java.io.InputStream;
import java.util.function.Supplier;

/** Authorized metadata with deferred stream acquisition; never serialized as an API response. */
public record SubmissionFileDownload(
        String filename,
        String mediaType,
        long sizeBytes,
        Supplier<InputStream> content
) {

    public InputStream openStream() {
        return content.get();
    }
}
