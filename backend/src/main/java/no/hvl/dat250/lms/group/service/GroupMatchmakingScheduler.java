package no.hvl.dat250.lms.group.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class GroupMatchmakingScheduler {
    private static final Logger log = LoggerFactory.getLogger(GroupMatchmakingScheduler.class);
    private final GroupMatchmakingService matchmaking;

    public GroupMatchmakingScheduler(GroupMatchmakingService matchmaking) {
        this.matchmaking = matchmaking;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 5_000)
    public void runDueMatchmaking() {
        for (Long courseId : matchmaking.dueCourses()) {
            try {
                matchmaking.run(courseId);
            } catch (RuntimeException exception) {
                log.error("Matchmaking failed for course {}. The scheduler will retry.", courseId, exception);
            }
        }
    }
}
