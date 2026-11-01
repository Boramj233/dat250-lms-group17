package no.hvl.dat250.lms.user.api.controllers;

import jakarta.validation.ConstraintViolationException;
import no.hvl.dat250.lms.user.service.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps user failures without exposing stored credentials or database messages. */
@RestControllerAdvice(assignableTypes = {UserController.class, AdminUserController.class})
public class UserExceptionHandler {
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ProblemDetail unauthenticated() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Operation not permitted");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail notFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail invalidProfile() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid profile details");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidRequest() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
    }
}
