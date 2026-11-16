package no.hvl.dat250.lms.submission.config;

import java.net.URI;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("lms.submission.storage")
public record SubmissionStorageProperties(
        @NotNull URI endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey) {
    // Never let a generated record toString expose credentials in diagnostics.
    @Override
    public String toString() {
        return "SubmissionStorageProperties[endpoint=" + endpoint + ", bucket=" + bucket + "]";
    }
}
