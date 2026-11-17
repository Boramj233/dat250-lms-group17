
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateStaticPageRequest;
import no.hvl.dat250.lms.content.api.dto.ReorderContentRequest;
import no.hvl.dat250.lms.content.api.dto.UpdateStaticPageRequest;
import no.hvl.dat250.lms.content.api.dto.ContentSummaryResponse;
import no.hvl.dat250.lms.content.api.dto.StaticPageResponse;
import no.hvl.dat250.lms.content.api.dto.ContentVisibilityResponse;
import no.hvl.dat250.lms.content.api.dto.UpdateContentVisibilityRequest;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.interaction.service.StudentInteractionService;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.auth.annotations.IsTeacherInCourse;
import no.hvl.dat250.lms.security.auth.annotations.IsEnrolledInCourse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PutMapping("/api/contents/{contentId}/visibility")
    public ResponseEntity<ContentVisibilityResponse> updateVisibility(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateContentVisibilityRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Content content;
        if (request.hasVisibleUntil()) {
            content = contentService.updateVisibilityWindow(contentId, user.userId(),
                request.getVisibleFrom(), request.getVisibleUntil());
        } else {
            content = contentService.updateVisibility(contentId, user.userId(), request.getVisibleFrom());
        }
        return ResponseEntity.ok(ContentVisibilityResponse.from(content));
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
            @PathVariable Long moduleId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        List<ContentSummaryResponse> contents =
                contentService.findContentsForUser(
                    courseId, moduleId, user.userId()
                )
                .stream()
                .map(ContentSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(contents);
    }

    // Temporary fix for completing content
    @PostMapping(MODULE_CONTENTS_PATH + "/{contentId}/complete")
    public ResponseEntity<Void> completeContent(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @PathVariable Long contentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {

        // TODO: BEFORE calling interactionService.completeContent(), verify:
        // 1. is the student enrolled at this course?
        // 2. is the content accessible?
        // 3. have the content-specific requirements been met?

        interactionService.completeContent(
                user.userId(),
                contentId
        );

        return ResponseEntity.noContent().build();
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

    @PreAuthorize("""
        @courseAccess.isEnrolled(authentication, @contentService.staticPageCourseId(#contentId))
        and (@courseAccess.isTeacher(authentication, @contentService.staticPageCourseId(#contentId))
            or @contentService.isVisible(#contentId))
        """)
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
