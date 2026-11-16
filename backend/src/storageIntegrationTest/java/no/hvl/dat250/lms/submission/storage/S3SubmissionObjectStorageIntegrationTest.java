package no.hvl.dat250.lms.submission.storage;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import no.hvl.dat250.lms.submission.config.SubmissionStorageConfig;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.jupiter.api.Assertions.*;

/** Actual S3 requests, including a new container using the same persisted data. */
class S3SubmissionObjectStorageIntegrationTest {
    private static final String IMAGE = "chrislusf/seaweedfs:4.53@sha256:ccad23e18639c8b98cee17e7100b95830a06077b56d8e5b006d6dcf8ec5e94a0";
    @TempDir Path data;

    private GenericContainer<?> container(String access, String secret) {
        return new GenericContainer<>(IMAGE)
                .withExposedPorts(8333)
                .withEnv("LMS_STORAGE_ACCESS_KEY", access)
                .withEnv("LMS_STORAGE_SECRET_KEY", secret)
                .withCopyFileToContainer(MountableFile.forHostPath(System.getProperty("storage.init-script")),
                        "/etc/lms/start.sh")
                .withFileSystemBind(data.toString(), "/data", BindMode.READ_WRITE)
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("/bin/sh", "/etc/lms/start.sh")
                        .withUser(hostUser()))
                .waitingFor(Wait.forLogMessage(".*All enabled components are running and ready to use:.*\\n", 1)
                        .withStartupTimeout(Duration.ofSeconds(90)));
    }

    private String hostUser() {
        try {
            return java.nio.file.Files.getAttribute(data, "unix:uid") + ":"
                    + java.nio.file.Files.getAttribute(data, "unix:gid");
        } catch (java.io.IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }

    private AnnotationConfigApplicationContext context(GenericContainer<?> container, String access, String secret) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("storage-test", java.util.Map.of(
                "lms.submission.storage.enabled", "true",
                "lms.submission.storage.endpoint", "http://" + container.getHost() + ":" + container.getMappedPort(8333),
                "lms.submission.storage.region", "us-east-1",
                "lms.submission.storage.bucket", "lms-submissions",
                "lms.submission.storage.access-key", access,
                "lms.submission.storage.secret-key", secret)));
        context.register(SubmissionStorageConfig.class);
        context.refresh();
        return context;
    }

    @Test
    void privateStorageRoundTripPersistsAcrossContainerRecreationAndSupportsCompensation() throws Exception {
        String access = "test" + UUID.randomUUID().toString().replace("-", "");
        String secret = UUID.randomUUID().toString().replace("-", "");
        String key = "submissions/" + UUID.randomUUID();
        byte[] bytes = "%PDF-1.7\nStorage integration test\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        SubmissionObjectStorage.ObjectReference reference;
        try (var container = container(access, secret)) {
            container.start();
            try (var context = context(container, access, secret)) {
                var storage = context.getBean(SubmissionObjectStorage.class);
                reference = storage.put(key, new ByteArrayInputStream(bytes), bytes.length, "application/pdf");
                try (var stream = storage.get(reference)) {
                    assertArrayEquals(bytes, stream.readAllBytes());
                }
                var s3 = context.getBean(S3Client.class);
                var metadata = s3.headObject(r -> r.bucket(reference.bucket()).key(reference.key()));
                assertEquals(bytes.length, metadata.contentLength());
                assertEquals("application/pdf", metadata.contentType());
                assertThrows(S3Exception.class, () -> s3.createBucket(r -> r.bucket("unauthorized-bucket")));
                assertThrows(IllegalArgumentException.class, () -> storage.get(
                        new SubmissionObjectStorage.ObjectReference("other-bucket", key)));
                try (var http = HttpClient.newHttpClient()) {
                    URI uri = URI.create("http://" + container.getHost() + ":" + container.getMappedPort(8333)
                            + "/lms-submissions/" + key);
                    assertEquals(403, http.send(HttpRequest.newBuilder(uri).GET().build(),
                            HttpResponse.BodyHandlers.discarding()).statusCode());
                    assertEquals(403, http.send(HttpRequest.newBuilder(uri).PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),
                            HttpResponse.BodyHandlers.discarding()).statusCode());
                }
            }
        }
        // New container, same bind-mounted /data: covers filer metadata AND object bytes.
        try (var container = container(access, secret)) {
            container.start();
            try (var context = context(container, access, secret)) {
                var storage = context.getBean(SubmissionObjectStorage.class);
                try (var stream = storage.get(reference)) {
                    assertArrayEquals(bytes, stream.readAllBytes());
                }
                storage.delete(reference);
                storage.delete(reference); // compensation can be repeated safely
                assertThrows(SubmissionStorageException.class, () -> storage.get(reference));
            }
        }
    }
}
