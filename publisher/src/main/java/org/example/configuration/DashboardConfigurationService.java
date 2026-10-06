package org.example.configuration;

import org.example.Security.DashboardDataStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DashboardConfigurationService {

    private final DashboardDataStore dataStore;

    public DashboardConfigurationService(DashboardDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Optional<SavedConfiguration> load() {
        return dataStore.loadConfiguration().map(config -> new SavedConfiguration(config.provider(),
                config.smtpHost(), config.smtpPort(), config.fromEmail(), config.smtpAuth(),
                config.smtpStarttls(), config.smtpUsername(), !config.smtpPassword().isBlank()));
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

        String yaml = toYaml(submitted, password);
        DashboardConfiguration saved = new DashboardConfiguration(submitted.provider(), submitted.smtpHost(),
                submitted.smtpPort(), submitted.fromEmail(), submitted.smtpAuth(), submitted.smtpStarttls(),
                submitted.smtpUsername(), submitted.smtpAuth() ? password : "");
        dataStore.saveConfiguration(saved);
        return yaml;
    }

    private String toYaml(DashboardConfiguration config, String password) {
        String host = quote(config.smtpHost(), "SMTP host");
        String from = quote(config.fromEmail(), "Sender address");
        StringBuilder yaml = new StringBuilder("spring:\n");
        if (config.provider() == QueueProvider.RABBITMQ) {
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
                                     boolean smtpPasswordConfigured) {}

    public static class InvalidConfigurationException extends RuntimeException {
        public InvalidConfigurationException(String message) {
            super(message);
        }
    }
}
