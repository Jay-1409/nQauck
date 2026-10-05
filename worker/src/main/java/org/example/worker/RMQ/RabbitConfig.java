package org.example.worker.RMQ;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "RABBITMQ", matchIfMissing = true)
public class RabbitConfig {

    @Bean
    public Queue emailQueue(@Value("${app.rabbitmq.queues.email.name:email.queue}") String queueName) {
        return new Queue(queueName, true);
    }
}
