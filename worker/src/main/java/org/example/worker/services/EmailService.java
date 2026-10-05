package org.example.worker.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.worker.entities.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${app.email.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(EmailRequest request) {
        if (request == null || isBlank(request.to()) || isBlank(request.subject()) || isBlank(request.htmlTemplate())) {
            throw new IllegalArgumentException("Email recipient, subject, and HTML body are required");
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(request.htmlTemplate(), true);
        } catch (MessagingException exception) {
            throw new MailPreparationException("Could not prepare email", exception);
        }
        System.out.println("event captured by worker");


        // uncomment later
        //mailSender.send(message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
