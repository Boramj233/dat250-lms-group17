package no.hvl.dat250.lms.content.api.controllers;

import no.hvl.dat250.lms.content.service.ContentPositionConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Database constraints also catch conflicting writes after a position availability check. */
@RestControllerAdvice(basePackageClasses = ContentController.class)
public class ContentConflictHandler {
    @ExceptionHandler({ContentPositionConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public void conflict() {
    }
}
