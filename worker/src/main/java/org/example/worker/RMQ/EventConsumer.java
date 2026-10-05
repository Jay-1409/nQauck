package org.example.worker.RMQ;

import org.example.worker.entities.EmailRequest;
import org.example.worker.services.EmailService;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private final EmailService emailService;

    public EventConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    public void consume(EmailRequest event) {
        emailService.send(event);
    }
}
