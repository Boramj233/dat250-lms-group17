
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateStaticPageRequest;
import no.hvl.dat250.lms.content.api.dto.ReorderContentRequest;
import no.hvl.dat250.lms.content.api.dto.UpdateStaticPageRequest;
import no.hvl.dat250.lms.content.api.dto.ContentSummaryResponse;
import no.hvl.dat250.lms.content.api.dto.StaticPageResponse;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.security.auth.annotations.IsTeacherInCourse;
import no.hvl.dat250.lms.security.auth.annotations.IsEnrolledInCourse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContentController {

    private static final String MODULE_CONTENTS_PATH =
        "/api/courses/{courseId}/modules/{moduleId}/contents";

    private final ContentService contentService;

    public ContentController(
            ContentService contentService) {

        this.contentService = contentService;
    }

    @IsTeacherInCourse
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

    @IsTeacherInCourse
    @PutMapping(MODULE_CONTENTS_PATH + "/order")
    public ResponseEntity<Void> reorder(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody ReorderContentRequest request) {
        contentService.reorder(courseId, moduleId, request.contentIds());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleInvalidArgument() {
        return ResponseEntity.badRequest().build();
    }

    @PreAuthorize("@courseAccess.isTeacher(authentication, @contentService.staticPageCourseId(#contentId))")
    @PutMapping("/api/contents/{contentId}")
    public ResponseEntity<StaticPageResponse> updateStaticPage(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateStaticPageRequest request) {
        StaticPage page = contentService.updateStaticPage(contentId, request.title(), request.body());
        return ResponseEntity.ok(StaticPageResponse.from(
            page, page.getModule().getCourse().getId(), page.getModule().getId()));
    }

    @IsEnrolledInCourse
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

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(
            NoSuchElementException exception) {

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> handleForbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PreAuthorize("@courseAccess.isEnrolled(authentication, @contentService.staticPageCourseId(#contentId))")
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
