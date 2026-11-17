package no.hvl.dat250.lms.submission.api.dto;

import no.hvl.dat250.lms.submission.domain.SubmissionMember;

public record SubmissionMemberResponse(Long userId, Long enrollmentId) {
    public static SubmissionMemberResponse from(SubmissionMember member) {
        return new SubmissionMemberResponse(member.getUserId(), member.getEnrollmentId());
    }
}
