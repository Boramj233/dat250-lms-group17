package no.hvl.dat250.lms.content.api.dto;

import no.hvl.dat250.lms.content.domain.StaticPage;

public record StaticPageResponse(
    Long id,
    Long courseId,
    Long moduleId,
    String title,
    int position,
    String body
) {

    public static StaticPageResponse from(
            StaticPage page,
            Long courseId,
            Long moduleId) {

        return new StaticPageResponse(
            page.getId(),
            courseId,
            moduleId,
            page.getTitle(),
            page.getPosition(),
            page.getBody()
        );
    }
}