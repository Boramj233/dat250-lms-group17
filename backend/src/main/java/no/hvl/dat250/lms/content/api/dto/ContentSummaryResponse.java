package no.hvl.dat250.lms.content.api.dto;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;

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

        if (content instanceof Quiz) {
            return new ContentSummaryResponse(
                content.getId(),
                "QUIZ",
                content.getTitle(),
                content.getPosition()
            );
        }

        if (content instanceof Assignment) {
            return new ContentSummaryResponse(
                content.getId(),
                "Assignment",
                content.getTitle(),
                content.getPosition()
            );
        }

        if (content instanceof CodingExercise) {
            return new ContentSummaryResponse(
                content.getId(),
                "CODING_EXERCISE",
                content.getTitle(),
                content.getPosition()
            );
        }

        throw new IllegalArgumentException(
            "Unsupported content type: "
                + content.getClass().getSimpleName());
    }
}