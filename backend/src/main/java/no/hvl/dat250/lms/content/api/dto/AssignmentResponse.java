package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

import no.hvl.dat250.lms.content.domain.Assignment;

public record AssignmentResponse(

    Long id,
    Long moduleId,
    String title,
    int position,
    String instructions,
    Instant deadline,
    boolean allowLateSubmission,
    boolean mandatoryForExam,
    Instant visibleFrom,
    Instant visibleUntil

) {

    public static AssignmentResponse from(
            Assignment assignment) {

        return new AssignmentResponse(
            assignment.getId(),
            assignment.getModule().getId(),
            assignment.getTitle(),
            assignment.getPosition(),
            assignment.getInstructions(),
            assignment.getDeadline(),
            assignment.isAllowLateSubmission(),
            assignment.isMandatoryForExam(),
            assignment.getVisibleFrom(),
            assignment.getVisibleUntil()
        );
    }
}
