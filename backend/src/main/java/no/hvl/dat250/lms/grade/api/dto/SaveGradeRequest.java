package no.hvl.dat250.lms.grade.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.constraints.*;

/**
 * @param visibleFrom when the grade becomes visible to the student by itself; null keeps
 *                    it hidden until it is released. Needs a Pass/Fail decision. A time
 *                    that has passed releases it at once.
 */
public record SaveGradeRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal score,
        @Size(max = 10000) String feedback,
        @PositiveOrZero Long version, Boolean passed, Instant visibleFrom) {
    public SaveGradeRequest(BigDecimal score, String feedback, Long version) {
        this(score, feedback, version, null, null);
    }

    public SaveGradeRequest(BigDecimal score, String feedback, Long version, Boolean passed) {
        this(score, feedback, version, passed, null);
    }
}
