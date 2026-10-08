package org.example.statistics;

import java.time.Instant;

public record PublisherStatisticsDto(
        Instant startedAt,
        Instant sampledAt,
        long requestsAccepted,
        long requestsFailed,
        double acceptanceRatePercent,
        long latencySampleCount,
        Double p50LatencyMillis,
        Double p95LatencyMillis,
        Double p99LatencyMillis) {}
