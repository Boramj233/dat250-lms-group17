package no.hvl.dat250.lms.submission.service;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.submission.storage.SubmissionStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileSubmissionServiceTest {

    private final SubmissionService submissions = mock(SubmissionService.class);
    private final SubmissionObjectStorage storage = mock(SubmissionObjectStorage.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<SubmissionObjectStorage> provider = mock(ObjectProvider.class);
    private final TestTransactionManager transactions = new TestTransactionManager();
    private final MockMultipartFile file = new MockMultipartFile(
            "file", "report.pdf", "application/pdf", "%PDF-1.7\nReport".getBytes());

    private FileSubmissionService service;

    @BeforeEach
    void setUp() {
        when(provider.getIfAvailable()).thenReturn(storage);
        service = new FileSubmissionService(
            submissions,
            new SubmissionFileValidator(DataSize.ofMegabytes(10)),
            provider,
            "lms-submissions",
            transactions,
            mock(EntityManager.class)
        );
    }

    private void saveSucceeds() {
        when(submissions.saveFileSubmission(eq(1L), eq(2L), any())).thenAnswer(invocation -> {
            SubmissionFileMetadata metadata = invocation.getArgument(2);
            return Submission.file(mock(Assignment.class), mock(Enrollment.class, RETURNS_DEEP_STUBS), metadata,
                    java.time.Instant.parse("2026-11-16T12:00:00Z"), false);
        });
    }

    @Test
    void successfulUploadRunsOutsideSqlTransactionAndRetainsObjectAfterCommit() {
        saveSucceeds();
        when(storage.put(anyString(), any(), anyLong(), anyString())).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return new SubmissionObjectStorage.ObjectReference("lms-submissions", invocation.getArgument(0));
        });

        var result = service.submitFile(1L, 2L, file);

        assertEquals("report.pdf", result.file().name());
        assertEquals(1, transactions.commits);
        verify(storage, never()).delete(any());
    }

    @Test
    void uploadFailureAttemptsDeletionAndNeverSavesSql() {
        when(storage.put(anyString(), any(), anyLong(), anyString()))
                .thenThrow(new SubmissionStorageException("upload", null));

        assertThrows(SubmissionStorageException.class, () -> service.submitFile(1L, 2L, file));

        verify(storage).delete(any());
        verify(submissions, never()).saveFileSubmission(any(), any(), any());
    }

    @Test
    void changedDeadlineAfterUploadRollsBackAndDeletesObject() {
        when(submissions.saveFileSubmission(eq(1L), eq(2L), any()))
                .thenThrow(new SubmissionDeadlinePassedException());

        assertThrows(SubmissionDeadlinePassedException.class, () -> service.submitFile(1L, 2L, file));

        verify(storage).delete(any());
        assertEquals(0, transactions.commits);
    }

    @Test
    void commitFailureWithConfirmedRollbackDeletesObject() {
        saveSucceeds();
        transactions.failCommit = true;
        transactions.setRollbackOnCommitFailure(true);

        assertThrows(TransactionSystemException.class, () -> service.submitFile(1L, 2L, file));

        verify(storage).delete(any());
    }

    @Test
    void uncertainCommitOutcomeRetainsObject() {
        saveSucceeds();
        transactions.failCommit = true;

        assertThrows(TransactionSystemException.class, () -> service.submitFile(1L, 2L, file));

        verify(storage, never()).delete(any());
    }

    @Test
    void failedCleanupDoesNotHideOriginalFailure() {
        when(submissions.saveFileSubmission(eq(1L), eq(2L), any()))
                .thenThrow(new SubmissionDeadlinePassedException());
        doThrow(new SubmissionStorageException("delete", null)).when(storage).delete(any());

        assertThrows(SubmissionDeadlinePassedException.class, () -> service.submitFile(1L, 2L, file));
    }

    @Test
    void unauthorizedUploadDoesNotReadFileOrContactStorage() {
        doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(submissions).validateFileSubmission(1L, 2L);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.submitFile(1L, 2L, file));

        verifyNoInteractions(storage);
    }

    @Test
    void transactionCannotStartDeletesObjectWithoutAttemptingSqlSave() {
        transactions.failBegin = true;

        assertThrows(org.springframework.transaction.CannotCreateTransactionException.class,
                () -> service.submitFile(1L, 2L, file));

        verify(storage).delete(any());
        verify(submissions, never()).saveFileSubmission(any(), any(), any());
    }

    @Test
    void disabledStorageReportsUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThrows(SubmissionStorageException.class, () -> service.submitFile(1L, 2L, file));

        verifyNoInteractions(storage);
    }

    @Test
    void downloadChecksOwnershipBeforeStorageAndDoesNotRequireCurrentEnrollment() {
        Submission submission = Submission.file(mock(Assignment.class), mock(Enrollment.class, RETURNS_DEEP_STUBS),
                new SubmissionFileMetadata("lms-submissions", "submissions/owned", "report.pdf",
                        "application/pdf", 123, "a".repeat(64)), java.time.Instant.now(), false);
        when(submissions.findOwnSubmission(1L, 2L)).thenReturn(submission);
        var stream = new java.io.ByteArrayInputStream(new byte[0]);
        when(storage.get(any())).thenReturn(stream);

        var download = service.findOwnFile(1L, 2L);

        verify(storage, never()).get(any());
        assertSame(stream, download.openStream());
        verify(submissions).findOwnSubmission(1L, 2L);
        verify(submissions, never()).validateFileSubmission(any(), any());
    }

    @Test
    void nonOwnerOrNonFileSubmissionCannotContactStorage() {
        when(submissions.findOwnSubmission(1L, 2L))
                .thenThrow(new java.util.NoSuchElementException("not found"));
        assertThrows(java.util.NoSuchElementException.class, () -> service.findOwnFile(1L, 2L));
        verifyNoInteractions(storage);

        doReturn(new Submission(
                mock(Assignment.class), mock(Enrollment.class, RETURNS_DEEP_STUBS),
                no.hvl.dat250.lms.submission.domain.SubmissionType.TEXT, "answer", java.time.Instant.now(), false))
                .when(submissions).findOwnSubmission(1L, 2L);
        assertThrows(java.util.NoSuchElementException.class, () -> service.findOwnFile(1L, 2L));
        verifyNoInteractions(storage);
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        boolean failCommit;
        boolean failBegin;
        int commits;

        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            if (failBegin) {
                throw new org.springframework.transaction.CannotCreateTransactionException("Injected begin failure");
            }
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            if (failCommit) {
                throw new TransactionSystemException("Injected commit failure");
            }
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
