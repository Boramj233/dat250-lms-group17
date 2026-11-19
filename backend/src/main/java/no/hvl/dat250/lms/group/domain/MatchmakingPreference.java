package no.hvl.dat250.lms.group.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record MatchmakingPreference(
        int targetGrade,
        int weeklyHours,
        @Enumerated(EnumType.STRING) Availability availability
) {
    public enum Availability {
        MORNING, AFTERNOON, EVENING, FLEXIBLE
    }

    public double distanceTo(MatchmakingPreference other) {
        double availabilityDistance = availability == other.availability
                || availability == Availability.FLEXIBLE
                || other.availability == Availability.FLEXIBLE ? 0 : 1;
        return Math.abs(targetGrade - other.targetGrade) / 4.0
                + Math.abs(weeklyHours - other.weeklyHours) / 19.0
                + availabilityDistance;
    }
}
