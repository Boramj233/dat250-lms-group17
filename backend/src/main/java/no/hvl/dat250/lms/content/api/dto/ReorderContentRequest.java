package no.hvl.dat250.lms.content.api.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReorderContentRequest(@NotNull List<@NotNull @Positive Long> contentIds) {
}
