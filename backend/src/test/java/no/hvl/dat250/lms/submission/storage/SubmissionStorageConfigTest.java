package no.hvl.dat250.lms.submission.storage;

import no.hvl.dat250.lms.submission.config.SubmissionStorageConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionStorageConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SubmissionStorageConfig.class);

    @Test
    void disabledStorageDoesNotRequireCredentialsOrServer() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(0, context.getBeansOfType(SubmissionObjectStorage.class).size());
        });
    }

    @Test
    void enabledStorageRejectsMissingCredentialsAtStartup() {
        runner.withPropertyValues("lms.submission.storage.enabled=true",
                "lms.submission.storage.endpoint=http://localhost:8333",
                "lms.submission.storage.region=us-east-1",
                "lms.submission.storage.bucket=lms-submissions")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
