package no.hvl.dat250.lms.submission.api.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.util.NoSuchElementException;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.submission.api.dto.SubmissionResponse;
import no.hvl.dat250.lms.submission.service.FileSubmissionService;
import no.hvl.dat250.lms.submission.service.SubmissionFileDownload;
import no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException;
import no.hvl.dat250.lms.submission.service.UnsupportedSubmissionFileException;
import no.hvl.dat250.lms.submission.storage.SubmissionStorageException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api")
public class SubmissionFileController {

    private final FileSubmissionService fileSubmissionService;

    public SubmissionFileController(
            FileSubmissionService fileSubmissionService) {

        this.fileSubmissionService = fileSubmissionService;
    }

    @PostMapping(
        value = "/contents/{contentId}/submissions/file",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<SubmissionResponse> submitFile(
            @PathVariable Long contentId,
            @RequestParam("file") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            MultipartHttpServletRequest request) {

        if (files.size() != 1 || request.getMultiFileMap().size() != 1) {
            throw new IllegalArgumentException("Exactly one file part named file is required");
        }

        if (request.getParameter("groupId") != null) {
            throw new IllegalArgumentException("Group file submissions are not supported yet");
        }

        SubmissionResponse response = fileSubmissionService.submitFile(
            contentId,
            currentUser.userId(),
            files.getFirst()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<Resource> downloadOwnFile(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        SubmissionFileDownload download = fileSubmissionService.findOwnFile(
            submissionId,
            currentUser.userId()
        );

        ResponseEntity.BodyBuilder response = downloadHeaders(download);
        InputStream input = download.openStream();
        Resource resource = new InputStreamResource(input);

        // ResourceHttpMessageConverter closes the stream, including on copy failure.
        return response.body(resource);
    }

    @RequestMapping(
        value = "/submissions/{submissionId}/file",
        method = RequestMethod.HEAD
    )
    public ResponseEntity<Void> inspectOwnFile(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        SubmissionFileDownload download = fileSubmissionService.findOwnFile(
            submissionId,
            currentUser.userId()
        );
        ResponseEntity.BodyBuilder response = downloadHeaders(download);

        // Check object availability, but do not read or send its body for HEAD.
        try (InputStream input = download.openStream()) {
            return response.build();
        } catch (IOException ex) {
            throw new SubmissionStorageException("download close", ex);
        }
    }

    private ResponseEntity.BodyBuilder downloadHeaders(
            SubmissionFileDownload download) {

        ContentDisposition disposition = ContentDisposition
            .attachment()
            .filename(download.filename(), StandardCharsets.UTF_8)
            .build();

        // Build all headers before acquiring the object stream.
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(download.mediaType()))
            .contentLength(download.sizeBytes())
            .cacheControl(CacheControl.noStore().cachePrivate())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header(HttpHeaders.ACCEPT_RANGES, "none")
            .header("X-Content-Type-Options", "nosniff");
    }

    @ExceptionHandler(SubmissionDeadlinePassedException.class)
    public ResponseEntity<ProblemDetail> handleDeadlinePassed() {
        return failure(HttpStatus.CONFLICT, "Submission deadline has passed");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied() {
        return failure(HttpStatus.FORBIDDEN, "Active student enrollment required");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound() {
        return failure(HttpStatus.NOT_FOUND, "Assignment or submission not found");
    }

    @ExceptionHandler(UnsupportedSubmissionFileException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedFile(
            UnsupportedSubmissionFileException exception) {

        return failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArgument(
            IllegalArgumentException exception) {

        return failure(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({SubmissionStorageException.class, DataAccessException.class, TransactionException.class})
    public ResponseEntity<ProblemDetail> handlePersistenceFailure() {
        return failure(HttpStatus.SERVICE_UNAVAILABLE, "File submission is temporarily unavailable; please try again later");
    }

    private ResponseEntity<ProblemDetail> failure(
            HttpStatus status,
            String detail) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        return ResponseEntity
            .status(status)
            .body(problem);
    }
}
