package no.hvl.dat250.lms.messaging;

import java.time.Instant;
import java.util.UUID;
import no.hvl.dat250.lms.submission.domain.SubmissionAccepted;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmissionEventPublisherTest {
    @Test
    void sendFailureCannotTurnAnAlreadyCommittedSubmissionIntoFailure() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        var event = new SubmissionAccepted(UUID.randomUUID(), 1L, 2L, Instant.now(), false);
        doThrow(new AmqpConnectException(new java.net.ConnectException("Broker unavailable")))
            .when(rabbit).convertAndSend(SubmissionMessagingConfig.EXCHANGE, SubmissionMessagingConfig.ROUTING_KEY, event);
        assertDoesNotThrow(() -> new SubmissionEventPublisher(rabbit).publish(event));
        verify(rabbit).convertAndSend(SubmissionMessagingConfig.EXCHANGE, SubmissionMessagingConfig.ROUTING_KEY, event);
    }
}
