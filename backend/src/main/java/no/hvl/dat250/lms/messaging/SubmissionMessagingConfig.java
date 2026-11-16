package no.hvl.dat250.lms.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "lms.messaging.enabled", havingValue = "true")
public class SubmissionMessagingConfig {
    public static final String EXCHANGE = "lms.submissions";
    public static final String ROUTING_KEY = "submission.accepted";
    public static final String QUEUE = "lms.submissions.activity";

    @Bean
    public Declarables submissionTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true);
        return new Declarables(exchange, queue,
            BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));
    }

    @Bean
    public JacksonJsonMessageConverter submissionMessageConverter() {
        return new JacksonJsonMessageConverter("no.hvl.dat250.lms.submission.domain");
    }
}
