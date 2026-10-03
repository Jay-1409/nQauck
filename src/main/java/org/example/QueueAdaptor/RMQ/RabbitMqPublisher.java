package org.example.QueueAdaptor.RMQ;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;

    public RabbitMqPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    public void publish(String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }
}
