package no.hvl.dat250.lms.content.api.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.hvl.dat250.lms.content.service.ContentCompletionService;
import no.hvl.dat250.lms.content.service.ContentCompletionService.Status;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/contents/{contentId}")
public class ContentCompletionController {
    private final ContentCompletionService completion;
    public ContentCompletionController(ContentCompletionService completion) { this.completion = completion; }
    public record Settings(@NotNull Boolean requiredForModuleCompletion, Integer passingPercentage) {}
    public record Mark(@NotNull Boolean completed) {}
    @PatchMapping("/completion-settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configure(@PathVariable Long contentId, @Valid @RequestBody Settings request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        completion.configure(contentId, user.userId(), request.requiredForModuleCompletion(), request.passingPercentage());
    }
    @GetMapping("/completion")
    public ResponseEntity<Status> status(@PathVariable Long contentId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(completion.status(contentId, user.userId()));
    }
    @PutMapping("/completion")
    public ResponseEntity<Status> mark(@PathVariable Long contentId, @Valid @RequestBody Mark request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
            .body(completion.markPage(contentId, user.userId(), request.completed()));
    }
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    void missing() {}
    @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    void forbidden() {}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    void invalid() {}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT)
    void conflict() {}
}
