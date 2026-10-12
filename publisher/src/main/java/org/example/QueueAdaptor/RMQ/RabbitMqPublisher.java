package org.example.QueueAdaptor.RMQ;

import org.example.QueueAdaptor.MessagePublisherAdapter;
import org.example.QueueAdaptor.QueueUnavailableException;
import org.example.configuration.QueueProvider;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqPublisher implements MessagePublisherAdapter {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;

    public RabbitMqPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.RABBITMQ;
    }

    @Override
    public void publish(String routingKey, Object message) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        } catch (AmqpConnectException exception) {
            throw new QueueUnavailableException("RabbitMQ is unavailable", exception);
        }
    }
}
