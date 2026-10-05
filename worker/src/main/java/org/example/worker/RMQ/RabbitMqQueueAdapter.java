package org.example.worker.RMQ;

import org.example.worker.QueueAdapter;
import org.example.worker.QueueProvider;
import org.example.worker.entities.EmailRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "RABBITMQ")
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
        // ponytail: SMTP failures requeue indefinitely; add backoff and a DLQ before production traffic.
        eventConsumer.consume(event);
    }
}
