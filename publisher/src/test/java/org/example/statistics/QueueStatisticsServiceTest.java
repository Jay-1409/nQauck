package org.example.statistics;

import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.QueueProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueueStatisticsServiceTest {

    @Test
    void usesKafkaAdapterWhenDashboardSelectsKafka() {
        DashboardConfigurationService configurationService = mock(DashboardConfigurationService.class);
        QueueStatisticsAdapter kafka = mock(QueueStatisticsAdapter.class);
        QueueStatisticsDto expected = new QueueStatisticsDto(QueueProvider.KAFKA, "email.topic", Instant.now(), 0L,
                null, null, null);
        when(configurationService.load()).thenReturn(Optional.of(new DashboardConfigurationService.SavedConfiguration(
                QueueProvider.KAFKA, "smtp.example.com", 587, "from@example.com", false, false, "", false,
                false, "localhost", 5672, "localhost:9092")));
        when(kafka.provider()).thenReturn(QueueProvider.KAFKA);
        when(kafka.getQueueStatistics()).thenReturn(expected);

        QueueStatisticsDto actual = new QueueStatisticsService(configurationService, List.of(kafka)).getQueueStatistics();

        assertSame(expected, actual);
    }
}
