package org.example.statistics;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

@Component
public class PublisherRequestMetrics {

    // ponytail: fixed ~1% log buckets approximate lifetime percentiles; use a rolling histogram for time-window stats.
    private static final int BUCKET_COUNT = 8192;
    private static final double LOG_BASE = Math.log(1.01);

    private final Instant startedAt = Instant.now();
    private final LongAdder accepted = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final AtomicLongArray latencyBuckets = new AtomicLongArray(BUCKET_COUNT);

    public void record(int status, long latencyNanos) {
        int bucket = (int) (Math.log(Math.max(1, latencyNanos)) / LOG_BASE);
        latencyBuckets.incrementAndGet(Math.min(bucket, BUCKET_COUNT - 1));
        if (status == HttpStatus.ACCEPTED.value()) {
            accepted.increment();
        } else {
            failed.increment();
        }
    }

    public PublisherStatisticsDto snapshot() {
        long acceptedCount = accepted.sum();
        long failedCount = failed.sum();
        long sampleCount = acceptedCount + failedCount;
        return new PublisherStatisticsDto(
                startedAt,
                Instant.now(),
                acceptedCount,
                failedCount,
                sampleCount == 0 ? 0 : acceptedCount * 100.0 / sampleCount,
                sampleCount,
                percentile(sampleCount, 0.50),
                percentile(sampleCount, 0.95),
                percentile(sampleCount, 0.99));
    }

    private Double percentile(long samples, double fraction) {
        if (samples == 0) {
            return null;
        }
        long wanted = (long) Math.ceil(samples * fraction);
        long seen = 0;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            seen += latencyBuckets.get(i);
            if (seen >= wanted) {
                return Math.pow(1.01, i + 1) / 1_000_000;
            }
        }
        return null;
    }
}
