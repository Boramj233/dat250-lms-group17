package no.hvl.dat250.lms.submission.api.dto;

import java.time.Instant;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;

/** Teacher view; storage coordinates are never exposed. */
public record SubmissionReviewResponse(
        Long id,
        Long contentId,
        Long studentUserId,
        Long enrollmentId,
        SubmissionType type,
        String payload,
        Instant submittedAt,
        boolean late,
        SubmissionFileResponse file,
        Long groupId,
        Long submittedByUserId,
        java.util.List<SubmissionMemberResponse> members) {

    public static SubmissionReviewResponse from(Submission submission) {
        var metadata = submission.getFile();
        SubmissionFileResponse file = metadata == null ? null : new SubmissionFileResponse(
            metadata.getOriginalFilename(), metadata.getMediaType(), metadata.getSizeBytes(),
            "/api/submissions/" + submission.getId() + "/review/file");
        return new SubmissionReviewResponse(
            submission.getId(), submission.getContent().getId(),
            submission.getSubmittedBy().getUser().getUserId(), submission.getSubmittedBy().getId(),
            submission.getType(), submission.getPayload(), submission.getSubmittedAt(),
            submission.isLate(), file,
            submission.getGroup() == null ? null : submission.getGroup().getGroupId(),
            submission.getSubmittedBy().getUser().getUserId(),
            submission.getMembers().stream().map(SubmissionMemberResponse::from).toList());
    }
}
