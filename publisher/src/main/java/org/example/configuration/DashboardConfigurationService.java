package org.example.configuration;

import org.example.Security.DashboardDataStore;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class DashboardConfigurationService {

    private static final String PRIORITY_SEQUENCE = "[0, 0, 0, 0, 1, 1, 2]";

    private final DashboardDataStore dataStore;
    private final Path publisherConfigurationFile;

    public DashboardConfigurationService(DashboardDataStore dataStore, Environment environment) {
        this.dataStore = dataStore;
        this.publisherConfigurationFile = Path.of(environment.getProperty("app.dashboard.data-dir",
                Path.of(System.getProperty("user.home"), ".pongpin").toString()))
                .resolve("publisher-application.yml");
    }

    public Optional<SavedConfiguration> load() {
        return dataStore.loadConfiguration().map(config -> new SavedConfiguration(config.provider(),
                config.smtpHost(), config.smtpPort(), config.fromEmail(), config.smtpAuth(),
                config.smtpStarttls(), config.smtpUsername(), !config.smtpPassword().isBlank(),
                config.priorityScheduling(), config.rabbitMqHost() == null ? "localhost" : config.rabbitMqHost(),
                config.rabbitMqPort() == 0 ? 5672 : config.rabbitMqPort(),
                config.kafkaBootstrapServers() == null ? "localhost:9092" : config.kafkaBootstrapServers()));
    }

    public String generate(DashboardConfiguration submitted) {
        if (submitted.smtpPort() < 1 || submitted.smtpPort() > 65535) {
            throw new InvalidConfigurationException("SMTP port must be between 1 and 65535");
        }

        DashboardConfiguration previous = dataStore.loadConfiguration().orElse(null);
        String password = submitted.smtpPassword();
        if (password.isBlank() && submitted.smtpAuth() && previous != null && previous.smtpAuth()) {
            password = previous.smtpPassword();
        }
        if (submitted.smtpAuth() && (isBlank(submitted.smtpUsername()) || isBlank(password))) {
            throw new InvalidConfigurationException("SMTP username and password are required when authentication is enabled");
        }

        if (submitted.priorityScheduling() && submitted.provider() != QueueProvider.RABBITMQ) {
            throw new InvalidConfigurationException("Priority scheduling is available only with RabbitMQ");
        }
        if (submitted.provider() == QueueProvider.RABBITMQ
                && (isBlank(submitted.rabbitMqHost()) || submitted.rabbitMqPort() < 1 || submitted.rabbitMqPort() > 65535)) {
            throw new InvalidConfigurationException("RabbitMQ host and a valid port are required");
        }
        if (submitted.provider() == QueueProvider.KAFKA && isBlank(submitted.kafkaBootstrapServers())) {
            throw new InvalidConfigurationException("Kafka bootstrap servers are required");
        }
        String yaml = toYaml(submitted, password);
        DashboardConfiguration saved = new DashboardConfiguration(submitted.provider(), submitted.smtpHost(),
                submitted.smtpPort(), submitted.fromEmail(), submitted.smtpAuth(), submitted.smtpStarttls(),
                submitted.smtpUsername(), submitted.smtpAuth() ? password : "", submitted.priorityScheduling(),
                submitted.rabbitMqHost(), submitted.rabbitMqPort(), submitted.kafkaBootstrapServers());
        dataStore.saveConfiguration(saved);
        writePublisherConfiguration(submitted);
        return yaml;
    }

    private void writePublisherConfiguration(DashboardConfiguration config) {
        StringBuilder yaml = new StringBuilder("app:\n  queue:\n    provider: ")
                .append(config.provider()).append('\n');
        switch (config.provider()) {
            case RABBITMQ -> yaml.append("""
                      spring:
                        rabbitmq:
                          host: %s
                          port: %d
                          username: ${RABBITMQ_USERNAME:guest}
                          password: ${RABBITMQ_PASSWORD:guest}
                    """.formatted(quote(config.rabbitMqHost(), "RabbitMQ host"), config.rabbitMqPort()));
            case KAFKA -> yaml.append("""
                    kafka:
                      bootstrap-servers: %s
                      topic: ${KAFKA_TOPIC:email.topic}
                    """.formatted(quote(config.kafkaBootstrapServers(), "Kafka bootstrap servers")));
            case SQS -> { }
        }
        writeAtomically(publisherConfigurationFile, yaml.toString());
    }

    private void writeAtomically(Path destination, String contents) {
        try {
            Files.createDirectories(destination.getParent());
            Path temporary = Files.createTempFile(destination.getParent(), ".publisher-application-", ".tmp");
            try {
                Files.writeString(temporary, contents, StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    Files.setPosixFilePermissions(destination, java.util.Set.of(
                            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException ignored) {
                    // shortcut: non-POSIX filesystems use the current user's default ACL.
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save publisher configuration YAML", exception);
        }
    }

    private String toYaml(DashboardConfiguration config, String password) {
        String host = quote(config.smtpHost(), "SMTP host");
        String from = quote(config.fromEmail(), "Sender address");
        StringBuilder yaml = new StringBuilder("spring:\n");
        if (config.provider() == QueueProvider.RABBITMQ) {
            String rabbitHost = quote("${RABBITMQ_HOST:" + config.rabbitMqHost() + "}", "RabbitMQ host");
            yaml.append("""
                      rabbitmq:
                        host: %s
                        port: ${RABBITMQ_PORT:%d}
                        username: ${RABBITMQ_USERNAME:guest}
                        password: ${RABBITMQ_PASSWORD:guest}
                    """.formatted(rabbitHost, config.rabbitMqPort()));
        }
        yaml.append("""
                  mail:
                    host: %s
                    port: %d
                """.formatted(host, config.smtpPort()));
        if (config.smtpAuth()) {
            yaml.append("    username: ").append(quote(config.smtpUsername(), "SMTP username")).append('\n');
            yaml.append("    password: ").append(quote(password, "SMTP password")).append('\n');
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
                """.formatted(config.smtpAuth(), config.smtpStarttls(), config.provider()));

        switch (config.provider()) {
            case RABBITMQ -> yaml.append("""
                      rabbitmq:
                        queues:
                          email:
                            name: email.queue
                          priority:
                            high: email.high.queue
                            medium: email.medium.queue
                            low: email.low.queue
                        priority-scheduling:
                          enabled: %s
                          sequence: %s
                    """.formatted(config.priorityScheduling(), PRIORITY_SEQUENCE));
            case KAFKA -> yaml.append("""
                      kafka:
                        bootstrap-servers: %s
                        topic: ${KAFKA_TOPIC:email.topic}
                    """.formatted(quote("${KAFKA_BOOTSTRAP_SERVERS:" + config.kafkaBootstrapServers() + "}",
                    "Kafka bootstrap servers")));
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
        if (isBlank(value) || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new InvalidConfigurationException(label + " must be a non-empty single line");
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record SavedConfiguration(QueueProvider provider, String smtpHost, int smtpPort, String fromEmail,
                                     boolean smtpAuth, boolean smtpStarttls, String smtpUsername,
                                     boolean smtpPasswordConfigured, boolean priorityScheduling,
                                     String rabbitMqHost, int rabbitMqPort, String kafkaBootstrapServers) {}

    public static class InvalidConfigurationException extends RuntimeException {
        public InvalidConfigurationException(String message) {
            super(message);
        }
    }
}
