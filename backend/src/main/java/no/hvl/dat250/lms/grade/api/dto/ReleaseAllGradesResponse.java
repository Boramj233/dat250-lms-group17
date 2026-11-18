package no.hvl.dat250.lms.grade.api.dto;

/**
 * The answer to releasing every grade of a content item at once.
 *
 * @param released grades that became visible to the students now
 * @param undecided saved grades left unreleased because they have no Pass/Fail decision
 */
public record ReleaseAllGradesResponse(int released, int undecided) {}
