package org.example.worker.RMQ;

import org.example.worker.QueueAdapter;
import org.example.worker.QueueProvider;
import org.example.worker.entities.EmailRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${app.queue.provider:}' == 'RABBITMQ' and '${app.rabbitmq.priority-scheduling.enabled:false}' == 'false'")
public class RabbitMqQueueAdapter implements QueueAdapter {

    private final EventConsumer eventConsumer;

    public RabbitMqQueueAdapter(EventConsumer eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.RABBITMQ;
    }

    @RabbitListener(queues = {
            "${app.rabbitmq.queues.email.name}",
            "${app.rabbitmq.queues.priority.high}",
            "${app.rabbitmq.queues.priority.medium}",
            "${app.rabbitmq.queues.priority.low}"
    })
    public void receive(EmailRequest event) {
        // shortcut: SMTP delivery errors requeue indefinitely, add exponential backoff and a dead-letter exchange before production.
        eventConsumer.consume(event);
    }
}
