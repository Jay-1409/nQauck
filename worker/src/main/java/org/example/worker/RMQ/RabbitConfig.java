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
            @Value("${app.rabbitmq.priority-scheduling.enabled:false}") boolean priorityScheduling,
            @Value("${app.rabbitmq.priority-scheduling.sequence:}") String[] sequence) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(emailQueue);
        if (priorityScheduling) {
            for (String queue : sequence) {
                if (queue.isBlank()) throw new IllegalArgumentException("Priority sequence contains an empty queue name");
                names.add(queue.trim());
            }
            if (names.size() == 1 && sequence.length == 0) {
                throw new IllegalArgumentException("Priority scheduling requires a non-empty queue sequence");
            }
        }
        return new Declarables(names.stream().map(name -> new Queue(name, true)).toList());
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
