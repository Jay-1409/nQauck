package org.example.Controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QueueConfigController {

    @PostMapping(value = "/config.yml", produces = "application/yaml")
    public ResponseEntity<String> generate(
            @RequestParam Provider provider,
            @RequestParam String smtpHost,
            @RequestParam int smtpPort,
            @RequestParam String fromEmail,
            @RequestParam(defaultValue = "false") boolean smtpAuth,
            @RequestParam(defaultValue = "false") boolean smtpStarttls,
            @RequestParam(defaultValue = "") String smtpUsername,
            @RequestParam(defaultValue = "") String smtpPassword) {
        if (smtpPort < 1 || smtpPort > 65535) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP port must be between 1 and 65535");
        }
        if (smtpAuth && (smtpUsername.isBlank() || smtpPassword.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP username and password are required when authentication is enabled");
        }

        String yaml = yaml(provider, smtpHost, smtpPort, fromEmail, smtpAuth, smtpStarttls, smtpUsername, smtpPassword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=application.yml")
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(yaml);
    }

    private String yaml(
            Provider provider,
            String smtpHost,
            int smtpPort,
            String fromEmail,
            boolean smtpAuth,
            boolean smtpStarttls,
            String smtpUsername,
            String smtpPassword) {
        String host = quote(smtpHost, "SMTP host");
        String from = quote(fromEmail, "Sender address");
        StringBuilder yaml = new StringBuilder("spring:\n");
        if (provider == Provider.RABBITMQ) {
            yaml.append("""
                      rabbitmq:
                        host: ${RABBITMQ_HOST:localhost}
                        port: ${RABBITMQ_PORT:5672}
                        username: ${RABBITMQ_USERNAME:guest}
                        password: ${RABBITMQ_PASSWORD:guest}
                    """);
        }
        yaml.append("""
                  mail:
                    host: %s
                    port: %d
                """.formatted(host, smtpPort));
        if (smtpAuth) {
            yaml.append("    username: ").append(quote(smtpUsername, "SMTP username")).append('\n');
            yaml.append("    password: ").append(quote(smtpPassword, "SMTP password")).append('\n');
        }
        yaml.append("""
                    properties:
                      "[mail.smtp.auth]": %s
                      "[mail.smtp.starttls.enable]": %s
                      "[mail.smtp.connectiontimeout]": 5000
                      "[mail.smtp.timeout]": 3000
                      "[mail.smtp.writetimeout]": 5000
                app:
                  queue:
                    provider: %s
                """.formatted(smtpAuth, smtpStarttls, provider));

        switch (provider) {
            case RABBITMQ -> yaml.append("""
                      rabbitmq:
                        queues:
                          email:
                            name: email.queue
                    """);
            case KAFKA -> yaml.append("""
                      kafka:
                        bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
                        topic: ${KAFKA_TOPIC:email.queue}
                    """);
            case SQS -> yaml.append("""
                      sqs:
                        region: ${AWS_REGION:ap-south-1}
                        queue-url: ${SQS_QUEUE_URL:}
                    """);
        }
        yaml.append("  email:\n    from: ").append(from).append('\n');
        return yaml.toString();
    }

    private String quote(String value, String label) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be a non-empty single line");
        }
        return "'" + value.replace("'", "''") + "'";
    }

    public enum Provider {
        RABBITMQ, KAFKA, SQS
    }
}
