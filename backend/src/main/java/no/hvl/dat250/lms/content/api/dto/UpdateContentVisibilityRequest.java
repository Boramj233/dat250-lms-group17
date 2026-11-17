package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;

/** PUT replaces the start restriction: null or omitted visibleFrom clears it. */
public record UpdateContentVisibilityRequest(Instant visibleFrom) {
}
