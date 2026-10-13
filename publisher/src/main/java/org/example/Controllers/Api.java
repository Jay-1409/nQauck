package org.example.Controllers;

import org.example.Entities.EmailRequest;
import org.example.QueueAdaptor.QueueUnavailableException;
import org.example.Security.AccessCoinStore;
import org.example.Services.MessagePublisherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class Api {

    private final MessagePublisherService messagePublisher;
    private final AccessCoinStore accessCoinStore;
    private final String emailRoutingKey;
    private final String highPriorityRoutingKey;
    private final String mediumPriorityRoutingKey;
    private final String lowPriorityRoutingKey;

    public Api(
            MessagePublisherService messagePublisher,
            AccessCoinStore accessCoinStore,
            @Value("${app.rabbitmq.queues.email.routing-key}") String emailRoutingKey,
            @Value("${app.rabbitmq.queues.priority.high.routing-key}") String highPriorityRoutingKey,
            @Value("${app.rabbitmq.queues.priority.medium.routing-key}") String mediumPriorityRoutingKey,
            @Value("${app.rabbitmq.queues.priority.low.routing-key}") String lowPriorityRoutingKey) {
        this.messagePublisher = messagePublisher;
        this.accessCoinStore = accessCoinStore;
        this.emailRoutingKey = emailRoutingKey;
        this.highPriorityRoutingKey = highPriorityRoutingKey;
        this.mediumPriorityRoutingKey = mediumPriorityRoutingKey;
        this.lowPriorityRoutingKey = lowPriorityRoutingKey;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(
            @RequestHeader(value = "X-Access-Coin", required = false) String accessCoinHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody EmailRequest request) {
        String token = resolveToken(accessCoinHeader, authHeader);
        if (token == null || !accessCoinStore.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                            "Missing or invalid AccessCoin. Provide a valid token in X-Access-Coin or Authorization: Bearer header."));
        }

        if (request == null || isBlank(request.to()) || isBlank(request.subject()) || isBlank(request.htmlTemplate())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                            "Email recipient, subject, and HTML body are required."));
        }
        if (!isValidEmail(request.to())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                            "Recipient address must be a valid email."));
        }

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

    private String resolveToken(String accessCoinHeader, String authHeader) {
        if (accessCoinHeader != null && !accessCoinHeader.isBlank()) {
            return accessCoinHeader.trim();
        }
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && at == email.lastIndexOf('@') && at < email.length() - 1 && !email.contains(" ");
    }
}
