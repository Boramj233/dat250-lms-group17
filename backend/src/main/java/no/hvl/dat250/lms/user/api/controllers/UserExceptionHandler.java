package no.hvl.dat250.lms.user.api.controllers;

import jakarta.validation.ConstraintViolationException;
import no.hvl.dat250.lms.user.api.InvalidUserCsvException;
import no.hvl.dat250.lms.user.service.CannotRemoveOwnAdminAccessException;
import no.hvl.dat250.lms.user.service.UserNotFoundException;
import no.hvl.dat250.lms.user.service.UserAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the user feature's own failures without exposing database messages. Missing or
 * insufficient credentials (401/403) are answered by the security feature.
 */
@RestControllerAdvice(assignableTypes = {UserController.class, AdminUserController.class})
public class UserExceptionHandler {
    @ExceptionHandler({UserAlreadyExistsException.class, DataIntegrityViolationException.class})
    public ProblemDetail duplicateProfile() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Profile data conflicts with an existing user");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @ExceptionHandler(CannotRemoveOwnAdminAccessException.class)
    public ProblemDetail cannotRemoveOwnAdminAccess(CannotRemoveOwnAdminAccessException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail notFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail invalidProfile() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid profile details");
    }

    /** The message is written by UserCsvReader and contains no stored data. */
    @ExceptionHandler(InvalidUserCsvException.class)
    public ProblemDetail invalidCsv(InvalidUserCsvException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidRequest() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
    }
}
