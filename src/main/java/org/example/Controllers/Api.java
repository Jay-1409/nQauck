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

    public Api(
            MessagePublisherService messagePublisher,
            @Value("${app.rabbitmq.queues.email.routing-key}") String emailRoutingKey) {
        this.messagePublisher = messagePublisher;
        this.emailRoutingKey = emailRoutingKey;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailRequest request) {
        messagePublisher.publish(emailRoutingKey, request);
        return ResponseEntity.accepted().build();
    }
}
