package no.hvl.dat250.lms.grade.api.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

/** version is null for creation; updates require the version returned by teacher review. */
public record SaveGradeRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal score,
        @Size(max = 10000) String feedback,
        @PositiveOrZero Long version) {}
