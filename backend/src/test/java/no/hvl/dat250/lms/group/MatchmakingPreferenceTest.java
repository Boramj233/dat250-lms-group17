package no.hvl.dat250.lms.group;

import no.hvl.dat250.lms.group.domain.MatchmakingPreference;
import org.junit.jupiter.api.Test;

import static no.hvl.dat250.lms.group.domain.MatchmakingPreference.Availability.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchmakingPreferenceTest {
    private static final double TOLERANCE = 1e-9;

    @Test
    void normalizesGradeAndHoursByTheirLargestPossibleGaps() {
        var baseline = new MatchmakingPreference(1, 1, MORNING);

        assertEquals(0.0, baseline.distanceTo(baseline), TOLERANCE);
        assertEquals(1.0, baseline.distanceTo(new MatchmakingPreference(5, 1, MORNING)), TOLERANCE);
        assertEquals(1.0, baseline.distanceTo(new MatchmakingPreference(1, 20, MORNING)), TOLERANCE);
        assertEquals(0.25 + 5.0 / 19, baseline.distanceTo(new MatchmakingPreference(2, 6, MORNING)), TOLERANCE);
        assertEquals(3.0, baseline.distanceTo(new MatchmakingPreference(5, 20, EVENING)), TOLERANCE);
    }

    @Test
    void flexibleAvailabilityAddsNoPenaltyInEitherDirection() {
        var morning = new MatchmakingPreference(3, 8, MORNING);
        var flexible = new MatchmakingPreference(3, 8, FLEXIBLE);
        var afternoon = new MatchmakingPreference(3, 8, AFTERNOON);

        assertEquals(0.0, morning.distanceTo(flexible), TOLERANCE);
        assertEquals(0.0, flexible.distanceTo(morning), TOLERANCE);
        assertEquals(1.0, morning.distanceTo(afternoon), TOLERANCE);
    }
}
