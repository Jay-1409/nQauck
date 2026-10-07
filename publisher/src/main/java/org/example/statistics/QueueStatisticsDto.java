package org.example.statistics;

import org.example.configuration.QueueProvider;

import java.time.Instant;

public record QueueStatisticsDto(
        QueueProvider provider,
        String queueName,
        Instant sampledAt,
        Long messagesWaiting,
        Long messagesUnacknowledged,
        Double publishRatePerSecond,
        Double deliveryRatePerSecond) {}
