package no.hvl.dat250.lms.submission.config;

import java.time.Duration;
import no.hvl.dat250.lms.submission.storage.S3SubmissionObjectStorage;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "lms.submission.storage.enabled", havingValue = "true")
@EnableConfigurationProperties(SubmissionStorageProperties.class)
public class SubmissionStorageConfig {
    @Bean(destroyMethod = "close")
    S3Client submissionS3Client(SubmissionStorageProperties p) {
        return S3Client.builder()
                .endpointOverride(p.endpoint()).region(Region.of(p.region()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(30)))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(60)))
                .build();
    }

    @Bean
    SubmissionObjectStorage submissionObjectStorage(S3Client submissionS3Client, SubmissionStorageProperties p) {
        return new S3SubmissionObjectStorage(submissionS3Client, p.bucket());
    }
}
