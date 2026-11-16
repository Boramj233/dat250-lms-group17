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
        SubmissionFileResponse file,
        Long groupId,
        Long submittedByUserId,
        java.util.List<SubmissionMemberResponse> members
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
            ),
            submission.getGroup() == null ? null : submission.getGroup().getGroupId(),
            submission.getSubmittedBy().getUser().getUserId(),
            submission.getMembers().stream().map(SubmissionMemberResponse::from).toList()
        );
    }
}
