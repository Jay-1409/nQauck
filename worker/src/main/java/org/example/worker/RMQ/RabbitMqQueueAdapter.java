package org.example.worker.RMQ;

import org.example.worker.QueueAdapter;
import org.example.worker.QueueProvider;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "RABBITMQ", matchIfMissing = true)
public class RabbitMqQueueAdapter implements QueueAdapter {

    private final EventConsumer eventConsumer;

    public RabbitMqQueueAdapter(EventConsumer eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.RABBITMQ;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.email.name:email.queue}")
    public void receive(String event) {
        eventConsumer.consume(event);
    }
}
