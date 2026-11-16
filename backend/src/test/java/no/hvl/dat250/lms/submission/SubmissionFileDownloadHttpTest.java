package no.hvl.dat250.lms.submission;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.submission.api.controllers.SubmissionFileController;
import no.hvl.dat250.lms.submission.service.FileSubmissionService;
import no.hvl.dat250.lms.submission.service.SubmissionFileDownload;
import no.hvl.dat250.lms.submission.storage.SubmissionStorageException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controller/resource lifecycle only; real JWT authorization is covered by the container tests. */
class SubmissionFileDownloadHttpTest {

    private final FileSubmissionService service = mock(FileSubmissionService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(new AuthenticatedUser(5L), null)
        );
        mvc = MockMvcBuilders
            .standaloneSetup(new SubmissionFileController(service))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulDownloadClosesStreamAndHeadClosesWithoutReading() throws Exception {
        byte[] bytes = "%PDF-1.7\nReport".getBytes();
        AtomicBoolean closed = new AtomicBoolean();
        when(service.findOwnFile(1L, 5L)).thenReturn(new SubmissionFileDownload(
            "report.pdf", "application/pdf", bytes.length,
            () -> new ByteArrayInputStream(bytes) {
                @Override
                public void close() throws IOException {
                    closed.set(true);
                    super.close();
                }
            }
        ));

        mvc.perform(get("/api/submissions/1/file"))
            .andExpect(status().isOk()).andExpect(content().bytes(bytes));
        assertTrue(closed.get());

        AtomicBoolean headClosed = new AtomicBoolean();
        when(service.findOwnFile(1L, 5L)).thenReturn(new SubmissionFileDownload(
            "report.pdf", "application/pdf", bytes.length,
            () -> new InputStream() {
                @Override
                public int read() {
                    fail("HEAD must not read the response body");
                    return -1;
                }

                @Override
                public void close() {
                    headClosed.set(true);
                }
            }
        ));
        mvc.perform(head("/api/submissions/1/file"))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[0]));
        assertTrue(headClosed.get());
    }

    @Test
    void failedResponseCopyStillClosesStream() {
        AtomicBoolean closed = new AtomicBoolean();
        when(service.findOwnFile(1L, 5L)).thenReturn(new SubmissionFileDownload(
            "report.pdf", "application/pdf", 123,
            () -> new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Injected stream failure");
                }

                @Override
                public void close() {
                    closed.set(true);
                }
            }
        ));

        assertThrows(Exception.class, () -> mvc.perform(get("/api/submissions/1/file")));
        assertTrue(closed.get());
    }

    @Test
    void storageFailureBeforeResponseBodyReturns503WithoutAttachmentHeaders() throws Exception {
        when(service.findOwnFile(1L, 5L)).thenReturn(new SubmissionFileDownload(
            "report.pdf", "application/pdf", 123,
            () -> { throw new SubmissionStorageException("download", null); }
        ));

        mvc.perform(get("/api/submissions/1/file"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().doesNotExist("Content-Disposition"));
    }
}
