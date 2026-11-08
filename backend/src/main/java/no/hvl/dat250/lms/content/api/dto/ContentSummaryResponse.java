package no.hvl.dat250.lms.content.api.dto;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;

public record ContentSummaryResponse(
    Long id,
    String type,
    String title,
    int position
) {

    public static ContentSummaryResponse from(Content content) {

        if (content instanceof StaticPage) {
            return new ContentSummaryResponse(
                content.getId(),
                "STATIC_PAGE",
                content.getTitle(),
                content.getPosition()
            );
        }

        throw new IllegalArgumentException(
            "Unsupported content type: "
                + content.getClass().getSimpleName());
    }
}