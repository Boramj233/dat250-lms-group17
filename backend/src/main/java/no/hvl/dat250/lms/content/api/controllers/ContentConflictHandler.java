package no.hvl.dat250.lms.content.api.controllers;

import no.hvl.dat250.lms.content.service.ContentPositionConflictException;
import no.hvl.dat250.lms.content.service.ContentDeletionConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Business guards and database constraints prevent conflicting or destructive writes. */
@RestControllerAdvice(basePackageClasses = ContentController.class)
public class ContentConflictHandler {
    @ExceptionHandler({ContentPositionConflictException.class, ContentDeletionConflictException.class,
        DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public void conflict() {
    }
}
