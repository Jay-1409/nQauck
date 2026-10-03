package org.example.Services;

import org.example.QueueAdaptor.RMQ.RabbitMqPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final RabbitMqPublisher rabbitMqPublisher;
    private final String routingKey;

    public EmailService(
            RabbitMqPublisher rabbitMqPublisher,
            @Value("${app.rabbitmq.queues.email.routing-key}") String routingKey) {
        this.rabbitMqPublisher = rabbitMqPublisher;
        this.routingKey = routingKey;
    }

    public void sendEmail(EmailRequest request) {
        rabbitMqPublisher.publish(routingKey, request);
    }

    public record EmailRequest(String to, String subject, String htmlTemplate) {}
}
