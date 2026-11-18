package no.hvl.dat250.lms.module.api.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The course's modules in their new order, first to last: PUT /api/courses/{courseId}/modules/order.
 * Every module of the course exactly once; their positions become 0, 1, 2, ….
 */
public record ReorderModulesRequest(@NotNull List<@NotNull @Positive Long> moduleIds) {
}
