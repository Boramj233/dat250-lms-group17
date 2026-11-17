package no.hvl.dat250.lms.submission.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Multipart parsing can fail before a Controller is selected, so this handler is global.
@RestControllerAdvice
public class SubmissionUploadSizeAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadTooLarge() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONTENT_TOO_LARGE,
            "Uploaded file or multipart request exceeds the configured size limit"
        );

        return ResponseEntity
            .status(HttpStatus.CONTENT_TOO_LARGE)
            .body(problem);
    }
}
