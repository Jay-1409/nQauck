package org.example.QueueAdaptor.RMQ;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.configuration.QueueProvider;
import org.example.statistics.QueueStatisticsDto;
import org.example.statistics.QueueStatisticsAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class RabbitMqStatisticsAdapter implements QueueStatisticsAdapter {

    private final RestClient restClient;
    private final String managementUrl;
    private final String username;
    private final String password;
    private final String queueName;

    public RabbitMqStatisticsAdapter(
            RabbitProperties rabbitProperties,
            @Value("${app.rabbitmq.management-url:}") String configuredManagementUrl,
            @Value("${app.rabbitmq.management-port:15672}") int managementPort,
            @Value("${app.rabbitmq.queues.email.name:email.queue}") String queueName) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.managementUrl = configuredManagementUrl.isBlank()
                ? "http://" + rabbitProperties.getHost() + ":" + managementPort
                : configuredManagementUrl;
        this.username = rabbitProperties.getUsername();
        this.password = rabbitProperties.getPassword();
        this.queueName = queueName;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.RABBITMQ;
    }

    @Override
    public QueueStatisticsDto getQueueStatistics() {
        String url = managementUrl.replaceAll("/+$", "")
                + "/api/queues/" + UriUtils.encodePathSegment("/", StandardCharsets.UTF_8)
                + "/" + UriUtils.encodePathSegment(queueName, StandardCharsets.UTF_8);
        JsonNode queue;
        try {
            queue = restClient.get()
                    .uri(URI.create(url))
                    .headers(headers -> headers.setBasicAuth(username, password))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not retrieve RabbitMQ queue statistics", exception);
        }
        if (queue == null) {
            throw new IllegalStateException("RabbitMQ Management API returned an empty queue response");
        }
        JsonNode messageStats = queue.path("message_stats");
        return new QueueStatisticsDto(
                provider(), queueName, Instant.now(),
                queue.path("messages_ready").asLong(),
                queue.path("messages_unacknowledged").asLong(),
                messageStats.path("publish_details").path("rate").asDouble(),
                messageStats.path("deliver_get_details").path("rate").asDouble());
    }
}
