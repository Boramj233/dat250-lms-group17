package no.hvl.dat250.lms.content.api.controllers;

import java.util.NoSuchElementException;
import no.hvl.dat250.lms.content.service.ContentDeletionService;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentDeletionController {
    private final ContentDeletionService deletion;

    public ContentDeletionController(ContentDeletionService deletion) {
        this.deletion = deletion;
    }

    @DeleteMapping("/api/contents/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long contentId, @AuthenticationPrincipal AuthenticatedUser user) {
        deletion.delete(contentId, user.userId());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void forbidden() {}

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void missing() {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalid() {}
}
