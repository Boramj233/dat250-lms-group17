package no.hvl.dat250.lms.messaging;

import no.hvl.dat250.lms.submission.domain.SubmissionAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "lms.messaging.enabled", havingValue = "true")
public class SubmissionActivityListener {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionActivityListener.class);

    // Duplicate delivery can produce duplicate log entries; there are no business mutations.
    @RabbitListener(queues = SubmissionMessagingConfig.QUEUE)
    public void record(SubmissionAccepted event) {
        logger.info("Submission accepted: eventId={}, submissionId={}, contentId={}, submittedAt={}, late={}",
            event.eventId(), event.submissionId(), event.contentId(), event.submittedAt(), event.late());
    }
}
