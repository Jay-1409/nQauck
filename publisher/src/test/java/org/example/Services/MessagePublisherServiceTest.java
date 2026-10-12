package org.example.Services;

import org.example.QueueAdaptor.MessagePublisherAdapter;
import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.QueueProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagePublisherServiceTest {

    @Test
    void usesProviderSavedFromDashboard() {
        DashboardConfigurationService configurationService = mock(DashboardConfigurationService.class);
        MessagePublisherAdapter rabbit = adapter(QueueProvider.RABBITMQ);
        MessagePublisherAdapter kafka = adapter(QueueProvider.KAFKA);
        when(configurationService.load()).thenReturn(Optional.of(new DashboardConfigurationService.SavedConfiguration(
                QueueProvider.KAFKA, "smtp.example.com", 587, "from@example.com", false, false, "", false,
                false, "localhost", 5672, "localhost:9092")));

        new MessagePublisherService(QueueProvider.RABBITMQ, configurationService, List.of(rabbit, kafka))
                .publish("email.send", "message");

        verify(kafka).publish("email.send", "message");
    }

    private MessagePublisherAdapter adapter(QueueProvider provider) {
        MessagePublisherAdapter adapter = mock(MessagePublisherAdapter.class);
        when(adapter.provider()).thenReturn(provider);
        return adapter;
    }
}
