package no.hvl.dat250.lms.submission.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubmissionClockConfig {

    @Bean("submissionClock")
    public Clock submissionClock() {
        return Clock.systemUTC();
    }
}
