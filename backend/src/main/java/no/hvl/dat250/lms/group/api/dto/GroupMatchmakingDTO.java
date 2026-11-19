package no.hvl.dat250.lms.group.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import no.hvl.dat250.lms.group.domain.MatchmakingPreference;
import no.hvl.dat250.lms.group.domain.MatchmakingPreference.Availability;

public record GroupMatchmakingDTO(
        Instant deadline,
        int maxGroupSize,
        boolean open,
        boolean completed,
        MatchmakingPreference preference,
        Participation participation
) {
    public record CreateRequest(@NotNull Instant deadline, @Min(1) int maxGroupSize) {}

    public record PreferenceRequest(
            @Min(1) @Max(5) int targetGrade,
            @Min(1) @Max(20) int weeklyHours,
            @NotNull Availability availability
    ) {
        public MatchmakingPreference toPreference() {
            return new MatchmakingPreference(targetGrade, weeklyHours, availability);
        }
    }

    public record Participation(long submitted, int totalStudents) {}
}
