
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateStaticPageRequest;
import no.hvl.dat250.lms.content.api.dto.ContentSummaryResponse;
import no.hvl.dat250.lms.content.api.dto.StaticPageResponse;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;

import no.hvl.dat250.lms.interaction.service.StudentInteractionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContentController {

    private static final String MODULE_CONTENTS_PATH =
        "/api/courses/{courseId}/modules/{moduleId}/contents";

    private final ContentService contentService;

    private final StudentInteractionService interactionService;

    public ContentController(
            ContentService contentService,
            StudentInteractionService interactionService) {

        this.contentService = contentService;
        this.interactionService = interactionService;
    }

    @PostMapping(MODULE_CONTENTS_PATH + "/pages")
    public ResponseEntity<StaticPageResponse> createStaticPage(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateStaticPageRequest request) {

        StaticPage page = contentService.createStaticPage(
                courseId,
                moduleId,
                request.title(),
                request.position(),
                request.body()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StaticPageResponse.from(
                    page, courseId, moduleId
                ));
    }

    @GetMapping(MODULE_CONTENTS_PATH)
    public ResponseEntity<List<ContentSummaryResponse>> getContents(
            @PathVariable Long courseId,
            @PathVariable Long moduleId) {

        List<ContentSummaryResponse> contents =
                contentService.findContentsByModule(
                    courseId, moduleId
                )
                .stream()
                .map(ContentSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(contents);
    }

    @GetMapping(MODULE_CONTENTS_PATH + "/{contentId}/complete")
    public ResponseEntity<Void> completeContent(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @PathVariable Long contentId
    ) {

        Long studentId = getAuthenticatedStudentId();

        interactionService.completeContent(
                studentId,
                contentId
        );

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(
            NoSuchElementException exception) {

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/contents/{contentId}")
    public ResponseEntity<StaticPageResponse> getStaticPage(
        @PathVariable Long contentId) {

    StaticPage page =
            contentService.findStaticPageById(contentId);

    Long moduleId = page.getModule().getId();
    Long courseId = page.getModule().getCourse().getId();

    return ResponseEntity.ok(
            StaticPageResponse.from(
                page,
                courseId,
                moduleId
            )
    );}
}
