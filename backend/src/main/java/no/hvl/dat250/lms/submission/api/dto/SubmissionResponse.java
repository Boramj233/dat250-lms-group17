package no.hvl.dat250.lms.submission.api.dto;

import java.time.Instant;

import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;

public record SubmissionResponse(
        Long id,
        Long contentId,
        SubmissionType type,
        String payload,
        Instant submittedAt,
        boolean late,
        SubmissionFileResponse file
) {

    public static SubmissionResponse from(
            Submission submission) {

        return new SubmissionResponse(
            submission.getId(),
            submission.getContent().getId(),
            submission.getType(),
            submission.getPayload(),
            submission.getSubmittedAt(),
            submission.isLate(),
            SubmissionFileResponse.from(
                submission.getFile(),
                submission.getId()
            )
        );
    }
}
