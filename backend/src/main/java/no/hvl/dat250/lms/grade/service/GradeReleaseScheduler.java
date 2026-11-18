package no.hvl.dat250.lms.grade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Releases grades whose visible date (SaveGradeRequest.visibleFrom) has come, so students
 * see them without the teacher pressing Release. Checks every minute by default
 * ({@code lms.grades.release-check-ms}); turn it off with
 * {@code lms.grades.release-scheduler.enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "lms.grades.release-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class GradeReleaseScheduler {
    private static final Logger log = LoggerFactory.getLogger(GradeReleaseScheduler.class);
    private final GradingService grading;

    public GradeReleaseScheduler(GradingService grading) {
        this.grading = grading;
    }

    @Scheduled(fixedDelayString = "${lms.grades.release-check-ms:60000}",
        initialDelayString = "${lms.grades.release-check-ms:60000}")
    public void releaseDueGrades() {
        try {
            int released = grading.releaseDue();
            if (released > 0) log.info("Released {} scheduled grade(s)", released);
        } catch (RuntimeException exception) {
            // Tried again on the next run; one failure must not stop the schedule.
            log.warn("Releasing scheduled grades failed", exception);
        }
    }
}
