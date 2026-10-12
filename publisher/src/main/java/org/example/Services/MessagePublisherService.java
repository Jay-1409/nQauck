package org.example.Services;

import org.example.QueueAdaptor.MessagePublisherAdapter;
import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.QueueProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MessagePublisherService {

    private final QueueProvider defaultProvider;
    private final DashboardConfigurationService configurationService;
    private final Map<QueueProvider, MessagePublisherAdapter> publishers;

    public MessagePublisherService(
            @Value("${app.queue.provider:RABBITMQ}") QueueProvider provider,
            DashboardConfigurationService configurationService,
            List<MessagePublisherAdapter> publishers) {
        this.defaultProvider = provider;
        this.configurationService = configurationService;
        this.publishers = publishers.stream().collect(Collectors.toUnmodifiableMap(
                MessagePublisherAdapter::provider, Function.identity()));
    }

    public void publish(String routingKey, Object message) {
        QueueProvider provider = configurationService.load()
                .map(DashboardConfigurationService.SavedConfiguration::provider)
                .orElse(defaultProvider);
        MessagePublisherAdapter publisher = publishers.get(provider);
        if (publisher == null) throw new IllegalStateException("No publisher is available for " + provider);
        publisher.publish(routingKey, message);
    }
}
