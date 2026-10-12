package org.example.QueueAdaptor.KAFKA;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.QueueProvider;
import org.example.statistics.QueueStatisticsAdapter;
import org.example.statistics.QueueStatisticsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaStatisticsAdapter implements QueueStatisticsAdapter {

    private static final long REQUEST_TIMEOUT_SECONDS = 3;

    private final DashboardConfigurationService configurationService;
    private final String defaultBootstrapServers;
    private final String topic;
    private final String groupId;
    private long previousPublished = -1;
    private long previousDelivered = -1;
    private Instant previousSampledAt;

    public KafkaStatisticsAdapter(
            DashboardConfigurationService configurationService,
            @Value("${app.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${app.kafka.topic:email.topic}") String topic,
            @Value("${app.kafka.group-id:pongpin-workers}") String groupId) {
        this.configurationService = configurationService;
        this.defaultBootstrapServers = bootstrapServers;
        this.topic = topic;
        this.groupId = groupId;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.KAFKA;
    }

    @Override
    public synchronized QueueStatisticsDto getQueueStatistics() {
        String bootstrapServers = configurationService.load()
                .map(DashboardConfigurationService.SavedConfiguration::kafkaBootstrapServers)
                .orElse(defaultBootstrapServers);
        Map<String, Object> settings = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) TimeUnit.SECONDS.toMillis(REQUEST_TIMEOUT_SECONDS),
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) TimeUnit.SECONDS.toMillis(REQUEST_TIMEOUT_SECONDS));
        try (AdminClient client = AdminClient.create(settings)) {
            List<TopicPartition> partitions = client.describeTopics(List.of(topic)).allTopicNames()
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS).get(topic).partitions().stream()
                    .map(partition -> new TopicPartition(topic, partition.partition()))
                    .toList();
            Map<TopicPartition, OffsetSpec> latestOffsets = new HashMap<>();
            for (TopicPartition partition : partitions) latestOffsets.put(partition, OffsetSpec.latest());
            Map<TopicPartition, Long> committedOffsets = client.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS).entrySet().stream()
                    .filter(entry -> entry.getKey().topic().equals(topic))
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().offset()));
            Map<TopicPartition, Long> latest = client.listOffsets(latestOffsets).all()
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS).entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().offset()));
            long published = latest.values().stream().mapToLong(Long::longValue).sum();
            long delivered = committedOffsets.values().stream().mapToLong(offset -> offset).sum();
            long waiting = latest.entrySet().stream().mapToLong(entry -> Math.max(0,
                    entry.getValue() - committedOffsets.getOrDefault(entry.getKey(), 0L))).sum();
            Instant sampledAt = Instant.now();
            Double publishRate = rate(previousPublished, published, previousSampledAt, sampledAt);
            Double deliveryRate = rate(previousDelivered, delivered, previousSampledAt, sampledAt);
            previousPublished = published;
            previousDelivered = delivered;
            previousSampledAt = sampledAt;
            return new QueueStatisticsDto(provider(), topic, sampledAt, waiting, null, publishRate, deliveryRate);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Kafka queue statistics request was interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not retrieve Kafka queue statistics", exception);
        }
    }

    private Double rate(long previous, long current, Instant previousAt, Instant currentAt) {
        if (previous < 0 || previousAt == null) return null;
        double seconds = (currentAt.toEpochMilli() - previousAt.toEpochMilli()) / 1000.0;
        return seconds <= 0 ? null : Math.max(0, current - previous) / seconds;
    }
}
