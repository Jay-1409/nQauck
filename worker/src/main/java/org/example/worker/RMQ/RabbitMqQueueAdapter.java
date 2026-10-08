package org.example.worker.RMQ;

import org.example.worker.QueueAdapter;
import org.example.worker.QueueProvider;
import org.example.worker.entities.EmailRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "RABBITMQ")
@ConditionalOnProperty(prefix = "app.rabbitmq.priority-scheduling", name = "enabled", havingValue = "false", matchIfMissing = true)
public class RabbitMqQueueAdapter implements QueueAdapter {

    private final EventConsumer eventConsumer;

    public RabbitMqQueueAdapter(EventConsumer eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.RABBITMQ;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.email.name}")
    public void receive(EmailRequest event) {
        eventConsumer.consume(event);
    }
}
