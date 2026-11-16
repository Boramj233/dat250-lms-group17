package no.hvl.dat250.lms.content.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAssignmentExamRequirementRequest(
        @NotNull Boolean mandatoryForExam) {
}
