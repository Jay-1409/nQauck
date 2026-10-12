package org.example.QueueAdaptor.KAFKA;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.QueueAdaptor.MessagePublisherAdapter;
import org.example.QueueAdaptor.QueueUnavailableException;
import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.QueueProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaPublisher implements MessagePublisherAdapter {

    private volatile KafkaProducer<String, String> producer;
    private final DashboardConfigurationService configurationService;
    private final String defaultBootstrapServers;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaPublisher(
            ObjectMapper objectMapper,
            DashboardConfigurationService configurationService,
            @Value("${app.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${app.kafka.topic:email.topic}") String topic) {
        this.configurationService = configurationService;
        this.defaultBootstrapServers = bootstrapServers;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.KAFKA;
    }

    @Override
    public void publish(String routingKey, Object message) {
        try {
            producer().send(new ProducerRecord<>(topic, routingKey, objectMapper.writeValueAsString(message))).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to Kafka", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RetriableException) {
                throw new QueueUnavailableException("Kafka is unavailable", exception);
            }
            throw new IllegalStateException("Could not publish to Kafka topic " + topic, exception);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not publish to Kafka topic " + topic, exception);
        }
    }

    @PreDestroy
    public void close() {
        KafkaProducer<String, String> activeProducer = producer;
        if (activeProducer != null) activeProducer.close();
    }

    private KafkaProducer<String, String> producer() {
        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    Properties properties = new Properties();
                    String bootstrapServers = configurationService.load()
                            .map(DashboardConfigurationService.SavedConfiguration::kafkaBootstrapServers)
                            .orElse(defaultBootstrapServers);
                    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                    producer = new KafkaProducer<>(properties);
                }
            }
        }
        return producer;
    }
}
