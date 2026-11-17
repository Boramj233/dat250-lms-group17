package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;
import no.hvl.dat250.lms.content.domain.Content;

public record ContentVisibilityResponse(Long contentId, Instant visibleFrom, Instant visibleUntil) {
    public static ContentVisibilityResponse from(Content content) {
        return new ContentVisibilityResponse(content.getId(), content.getVisibleFrom(), content.getVisibleUntil());
    }
}
