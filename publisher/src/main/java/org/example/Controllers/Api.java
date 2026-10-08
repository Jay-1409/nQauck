package org.example.Controllers;

import org.example.Services.MessagePublisherService;
import org.example.Entities.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class Api {

    private final MessagePublisherService messagePublisher;
    private final String emailRoutingKey;
    private final String highPriorityRoutingKey;
    private final String mediumPriorityRoutingKey;
    private final String lowPriorityRoutingKey;

    public Api(
            MessagePublisherService messagePublisher,
            @Value("${app.rabbitmq.queues.email.routing-key}") String emailRoutingKey,
            @Value("${app.rabbitmq.queues.priority.high.routing-key}") String highPriorityRoutingKey,
            @Value("${app.rabbitmq.queues.priority.medium.routing-key}") String mediumPriorityRoutingKey,
            @Value("${app.rabbitmq.queues.priority.low.routing-key}") String lowPriorityRoutingKey) {
        this.messagePublisher = messagePublisher;
        this.emailRoutingKey = emailRoutingKey;
        this.highPriorityRoutingKey = highPriorityRoutingKey;
        this.mediumPriorityRoutingKey = mediumPriorityRoutingKey;
        this.lowPriorityRoutingKey = lowPriorityRoutingKey;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailRequest request) {
        String routingKey = request.priority() == null ? emailRoutingKey : switch (request.priority()) {
            case HIGH -> highPriorityRoutingKey;
            case MEDIUM -> mediumPriorityRoutingKey;
            case LOW -> lowPriorityRoutingKey;
        };
        messagePublisher.publish(routingKey, request);
        return ResponseEntity.accepted().build();
    }
}
