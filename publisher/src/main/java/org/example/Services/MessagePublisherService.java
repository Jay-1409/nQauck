package org.example.Services;

import org.example.QueueAdaptor.RMQ.RabbitMqPublisher;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisherService {

    private final RabbitMqPublisher rabbitMqPublisher;

    public MessagePublisherService(RabbitMqPublisher rabbitMqPublisher) {
        this.rabbitMqPublisher = rabbitMqPublisher;
    }

    public void publish(String routingKey, Object message) {
        rabbitMqPublisher.publish(routingKey, message);
    }
}
