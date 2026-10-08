package org.example.worker.RMQ;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.LinkedHashSet;

@Configuration
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "RABBITMQ")
public class RabbitConfig {

    @Bean
    public Declarables queues(
            @Value("${app.rabbitmq.queues.email.name}") String emailQueue,
            @Value("${app.rabbitmq.queues.priority.high}") String highQueue,
            @Value("${app.rabbitmq.queues.priority.medium}") String mediumQueue,
            @Value("${app.rabbitmq.queues.priority.low}") String lowQueue) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(emailQueue);
        names.add(highQueue);
        names.add(mediumQueue);
        names.add(lowQueue);
        return new Declarables(names.stream().map(name -> new Queue(name, true)).toList());
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
