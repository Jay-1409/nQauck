package org.example.Controllers;

import org.example.Services.MessagePublisherService;
import org.example.Entities.EmailRequest;
import org.example.QueueAdaptor.QueueUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
        String routingKey = request.priority() == null ? emailRoutingKey : switch (request.priority()) {
            case HIGH -> highPriorityRoutingKey;
            case MEDIUM -> mediumPriorityRoutingKey;
            case LOW -> lowPriorityRoutingKey;
        };
        try {
            messagePublisher.publish(routingKey, request);
        } catch (QueueUnavailableException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                            "The message queue is unavailable. Please try again later."));
        }
        return ResponseEntity.accepted().build();
    }
}
