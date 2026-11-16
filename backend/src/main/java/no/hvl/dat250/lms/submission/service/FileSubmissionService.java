package no.hvl.dat250.lms.submission.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.submission.api.dto.SubmissionResponse;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage.ObjectReference;
import no.hvl.dat250.lms.submission.storage.SubmissionStorageException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(FileSubmissionService.class);

    private final SubmissionService submissionService;
    private final SubmissionFileValidator fileValidator;
    private final ObjectProvider<SubmissionObjectStorage> storageProvider;
    private final String bucket;
    private final TransactionTemplate writeTransaction;
    private final EntityManager entityManager;

    public FileSubmissionService(
            SubmissionService submissionService,
            SubmissionFileValidator fileValidator,
            ObjectProvider<SubmissionObjectStorage> storageProvider,
            @Value("${lms.submission.storage.bucket:lms-submissions}") String bucket,
            PlatformTransactionManager transactionManager,
            EntityManager entityManager) {

        this.entityManager = entityManager;
        this.submissionService = submissionService;
        this.fileValidator = fileValidator;
        this.storageProvider = storageProvider;
        this.bucket = bucket;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Never hold a caller's SQL transaction open while sending a file over the network.
    @Transactional(propagation = Propagation.NEVER)
    public SubmissionResponse submitFile(
            Long contentId,
            Long userId,
            MultipartFile file) {

        submissionService.validateFileSubmission(contentId, userId);

        SubmissionObjectStorage storage = storageProvider.getIfAvailable();

        if (storage == null) {
            throw new SubmissionStorageException("unavailable", null);
        }

        SubmissionFileValidator.ValidatedFile validated;

        try {
            validated = fileValidator.validate(file);
        } catch (IOException ex) {
            throw new SubmissionStorageException("file read", ex);
        }

        // Know the key before uploading so even an ambiguous upload failure can be cleaned up.
        String key = "submissions/" + UUID.randomUUID();
        ObjectReference reference = new ObjectReference(bucket, key);
        SubmissionFileMetadata metadata = new SubmissionFileMetadata(
            bucket,
            key,
            validated.filename(),
            validated.mediaType(),
            validated.sizeBytes(),
            validated.sha256()
        );

        try (var input = file.getInputStream()) {
            storage.put(key, input, validated.sizeBytes(), validated.mediaType());
        } catch (IOException ex) {
            deleteAfterFailure(storage, reference);
            throw new SubmissionStorageException("upload read", ex);
        } catch (RuntimeException ex) {
            deleteAfterFailure(storage, reference);
            throw ex;
        }

        AtomicBoolean cleanupRegistered = new AtomicBoolean(false);

        try {
            return writeTransaction.execute(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int completionStatus) {
                        if (completionStatus == STATUS_ROLLED_BACK) {
                            deleteAfterFailure(storage, reference);
                        } else if (completionStatus == STATUS_UNKNOWN) {
                            // A committed SQL row may exist. Preserve the object for reconciliation.
                            logger.error("Unknown SQL commit outcome; retain submission object {}/{}",
                                reference.bucket(), reference.key());
                        }
                    }
                });
                cleanupRegistered.set(true);

                // Discard any preflight entities retained by Open EntityManager in View.
                // Rechecking must read current Enrollment and deadline, not a cached entity.
                entityManager.clear();

                Submission saved = submissionService.saveFileSubmission(
                    contentId,
                    userId,
                    metadata
                );

                // Map lazy Content fields before the short SQL transaction closes.
                return SubmissionResponse.from(saved);
            });
        } catch (RuntimeException ex) {
            if (!cleanupRegistered.get()) {
                // Transaction could not start; no SQL save was attempted.
                deleteAfterFailure(storage, reference);
            }

            throw ex;
        }
    }

    @Transactional(propagation = Propagation.NEVER)
    public SubmissionFileDownload findOwnFile(
            Long submissionId,
            Long userId) {

        // The owner-scoped SQL query must succeed before any object storage access.
        Submission submission = submissionService.findOwnSubmission(submissionId, userId);
        SubmissionFileMetadata file = submission.getFile();

        if (submission.getType() != SubmissionType.FILE || file == null) {
            throw new NoSuchElementException("Submission file not found");
        }

        SubmissionObjectStorage storage = storageProvider.getIfAvailable();

        if (storage == null) {
            throw new SubmissionStorageException("unavailable", null);
        }

        ObjectReference reference = new ObjectReference(
            file.getBucket(),
            file.getObjectKey()
        );

        // The SQL read transaction has ended. The Controller opens the stream after
        // constructing headers, and Spring closes it after copying the response body.
        return new SubmissionFileDownload(
            file.getOriginalFilename(),
            file.getMediaType(),
            file.getSizeBytes(),
            () -> openDownload(storage, reference, submissionId)
        );
    }

    private InputStream openDownload(
            SubmissionObjectStorage storage,
            ObjectReference reference,
            Long submissionId) {

        try {
            return storage.get(reference);
        } catch (SubmissionStorageException ex) {
            logger.error("Could not download file for submission {}", submissionId, ex);
            throw ex;
        }
    }

    private void deleteAfterFailure(
            SubmissionObjectStorage storage,
            ObjectReference reference) {

        try {
            storage.delete(reference);
        } catch (RuntimeException cleanupFailure) {
            // Preserve the original failure and leave enough information for orphan cleanup.
            logger.error("Could not delete orphan submission object {}/{}",
                reference.bucket(), reference.key(), cleanupFailure);
        }
    }
}
