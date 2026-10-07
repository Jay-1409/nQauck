package org.example.statistics;

import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.QueueProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QueueStatisticsService {

    private final DashboardConfigurationService configurationService;
    private final Map<QueueProvider, QueueStatisticsAdapter> adapters;

    public QueueStatisticsService(
            DashboardConfigurationService configurationService,
            List<QueueStatisticsAdapter> adapters) {
        this.configurationService = configurationService;
        this.adapters = adapters.stream().collect(Collectors.toUnmodifiableMap(
                QueueStatisticsAdapter::provider, Function.identity()));
    }

    public QueueStatisticsDto getQueueStatistics() {
        QueueProvider provider = configurationService.load()
                .map(DashboardConfigurationService.SavedConfiguration::provider)
                .orElse(QueueProvider.RABBITMQ);
        QueueStatisticsAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                    "Queue statistics are not available for " + provider);
        }
        return adapter.getQueueStatistics();
    }
}
