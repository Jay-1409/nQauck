package org.example.statistics;

import org.example.configuration.QueueProvider;

public interface QueueStatisticsAdapter {
    QueueProvider provider();

    QueueStatisticsDto getQueueStatistics();
}
