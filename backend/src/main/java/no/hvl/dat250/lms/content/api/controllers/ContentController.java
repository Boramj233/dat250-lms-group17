
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateStaticPageRequest;
import no.hvl.dat250.lms.content.api.dto.ContentSummaryResponse;
import no.hvl.dat250.lms.content.api.dto.StaticPageResponse;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    "/api/courses/{courseId}/modules/{moduleId}/contents"
)
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping("/pages")
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

    @GetMapping
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

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(
            NoSuchElementException exception) {

        return ResponseEntity.notFound().build();
    }
}
