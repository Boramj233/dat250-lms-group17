package no.hvl.dat250.lms.messaging;

import no.hvl.dat250.lms.submission.domain.SubmissionAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "lms.messaging.enabled", havingValue = "true")
public class SubmissionEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public SubmissionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Never publish rolled-back submissions. No fallback execution outside a transaction.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(SubmissionAccepted event) {
        try {
            rabbitTemplate.convertAndSend(SubmissionMessagingConfig.EXCHANGE,
                SubmissionMessagingConfig.ROUTING_KEY, event);
        } catch (RuntimeException failure) {
            // SQL is already committed. Do not report the accepted submission as failed.
            // Best effort only: no outbox, replay, or guaranteed delivery.
            logger.error("Could not publish submission event {} for submission {}; no automatic replay",
                event.eventId(), event.submissionId(), failure);
        }
    }
}
