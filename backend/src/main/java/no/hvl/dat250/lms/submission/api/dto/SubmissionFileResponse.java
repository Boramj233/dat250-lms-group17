package no.hvl.dat250.lms.submission.api.dto;

import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;

/** Public metadata only: never expose the bucket or object key. */
public record SubmissionFileResponse(
        String name,
        String mediaType,
        long sizeBytes,
        String downloadPath
) {

    public static SubmissionFileResponse from(
            SubmissionFileMetadata file,
            Long submissionId) {

        if (file == null) {
            return null;
        }

        return new SubmissionFileResponse(
            file.getOriginalFilename(),
            file.getMediaType(),
            file.getSizeBytes(),
            "/api/submissions/" + submissionId + "/file"
        );
    }
}
