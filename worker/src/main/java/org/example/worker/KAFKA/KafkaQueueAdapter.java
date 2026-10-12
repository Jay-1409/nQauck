package org.example.worker.KAFKA;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.worker.QueueAdapter;
import org.example.worker.QueueProvider;
import org.example.worker.RMQ.EventConsumer;
import org.example.worker.entities.EmailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "KAFKA")
public class KafkaQueueAdapter implements QueueAdapter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaQueueAdapter.class);

    private final EventConsumer eventConsumer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String bootstrapServers;
    private final String topic;
    private final String groupId;
    private volatile boolean running;
    private volatile KafkaConsumer<String, String> consumer;
    private Thread worker;

    public KafkaQueueAdapter(
            EventConsumer eventConsumer,
            @Value("${app.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${app.kafka.topic:email.topic}") String topic,
            @Value("${app.kafka.group-id:pongpin-workers}") String groupId) {
        this.eventConsumer = eventConsumer;
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.groupId = groupId;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.KAFKA;
    }

    @PostConstruct
    void start() {
        running = true;
        worker = Thread.ofPlatform().name("kafka-email-consumer").start(this::consume);
    }

    @PreDestroy
    void stop() throws InterruptedException {
        running = false;
        KafkaConsumer<String, String> activeConsumer = consumer;
        if (activeConsumer != null) activeConsumer.wakeup();
        if (worker != null) worker.join();
    }

    private void consume() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        while (running) {
            try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
                consumer = kafkaConsumer;
                kafkaConsumer.subscribe(List.of(topic));
                while (running) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1));
                    Set<TopicPartition> failedPartitions = new HashSet<>();
                    for (ConsumerRecord<String, String> record : records) {
                        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
                        if (failedPartitions.contains(partition)) continue;
                        try {
                            EmailRequest event = objectMapper.readValue(record.value(), EmailRequest.class);
                            eventConsumer.consume(event);
                            kafkaConsumer.commitSync(Map.of(partition, new OffsetAndMetadata(record.offset() + 1)));
                        } catch (Exception exception) {
                            logger.error("Failed to process Kafka record at {}-{} offset {}", record.topic(),
                                    record.partition(), record.offset(), exception);
                            // shortcut: a poison record blocks its partition; add a dead-letter topic if poison-message handling is needed.
                            kafkaConsumer.seek(partition, record.offset());
                            failedPartitions.add(partition);
                            Thread.sleep(1000);
                        }
                    }
                }
            } catch (WakeupException exception) {
                if (running) logger.warn("Kafka consumer was woken unexpectedly", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException exception) {
                if (running) logger.error("Kafka consumer failed; retrying", exception);
            } finally {
                consumer = null;
            }
            if (running) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
