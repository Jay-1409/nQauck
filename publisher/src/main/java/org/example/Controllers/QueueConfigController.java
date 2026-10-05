package org.example.Controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueueConfigController {

    @GetMapping(value = "/config.yml", produces = "application/yaml")
    public ResponseEntity<String> generate(@RequestParam Provider provider) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=application.yml")
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(provider.yaml());
    }

    public enum Provider {
        RABBITMQ, KAFKA, SQS;

        private String yaml() {
            return switch (this) {
                case RABBITMQ -> """
                        spring:
                          rabbitmq:
                            host: ${RABBITMQ_HOST:localhost}
                            port: ${RABBITMQ_PORT:5672}
                            username: ${RABBITMQ_USERNAME:guest}
                            password: ${RABBITMQ_PASSWORD:guest}
                        app:
                          queue:
                            provider: RABBITMQ
                          rabbitmq:
                            queues:
                              email:
                                name: email.queue
                        """;
                case KAFKA -> """
                        app:
                          queue:
                            provider: KAFKA
                            kafka:
                              bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
                              topic: ${KAFKA_TOPIC:email.queue}
                        """;
                case SQS -> """
                        app:
                          queue:
                            provider: SQS
                            sqs:
                              region: ${AWS_REGION:ap-south-1}
                              queue-url: ${SQS_QUEUE_URL:}
                        """;
            };
        }
    }
}
