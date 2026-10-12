package org.example.statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublisherRequestMetricsPersistenceTest {

    @Test
    void restoresCountersAndLatencySamples(@TempDir Path directory) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.dashboard.data-dir", directory.toString());
        PublisherRequestMetrics first = new PublisherRequestMetrics(environment);
        first.record(202, 1_000_000);
        first.record(503, 2_000_000);
        first.persistNow();

        PublisherRequestMetrics restored = new PublisherRequestMetrics(environment);
        PublisherStatisticsDto snapshot = restored.snapshot();

        assertEquals(1, snapshot.requestsAccepted());
        assertEquals(1, snapshot.requestsFailed());
        assertEquals(2, snapshot.latencySampleCount());
    }
}
