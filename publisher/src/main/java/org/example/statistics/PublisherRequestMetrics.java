package org.example.statistics;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

@Component
public class PublisherRequestMetrics {

    private static final Logger logger = LoggerFactory.getLogger(PublisherRequestMetrics.class);
    private static final int BUCKET_COUNT = 8192;
    private static final double LOG_BASE = Math.log(1.01);
    private static final long PERSIST_INTERVAL_SECONDS = 10;

    private final Path statisticsFile;
    private final Instant startedAt;
    private final LongAdder accepted = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final AtomicLongArray latencyBuckets = new AtomicLongArray(BUCKET_COUNT);
    private final ScheduledExecutorService persistence = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "publisher-statistics-persistence");
        thread.setDaemon(true);
        return thread;
    });
    private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    public PublisherRequestMetrics(Environment environment) {
        Path directory = Path.of(environment.getProperty("app.dashboard.data-dir",
                Path.of(System.getProperty("user.home"), ".pongpin").toString()));
        this.statisticsFile = directory.resolve("publisher-statistics.yml");
        this.startedAt = load();
    }

    @PostConstruct
    void startPersistence() {
        persistence.scheduleAtFixedRate(this::persistSafely, PERSIST_INTERVAL_SECONDS,
                PERSIST_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopPersistence() {
        persistSafely();
        persistence.shutdownNow();
    }

    public void record(int status, long latencyNanos) {
        int bucket = (int) (Math.log(Math.max(1, latencyNanos)) / LOG_BASE);
        latencyBuckets.incrementAndGet(Math.min(bucket, BUCKET_COUNT - 1));
        if (status == HttpStatus.ACCEPTED.value()) accepted.increment();
        else failed.increment();
    }

    public PublisherStatisticsDto snapshot() {
        long acceptedCount = accepted.sum();
        long failedCount = failed.sum();
        long sampleCount = acceptedCount + failedCount;
        return new PublisherStatisticsDto(
                startedAt, Instant.now(), acceptedCount, failedCount,
                sampleCount == 0 ? 0 : acceptedCount * 100.0 / sampleCount,
                sampleCount, percentile(sampleCount, 0.50), percentile(sampleCount, 0.95),
                percentile(sampleCount, 0.99));
    }

    private Instant load() {
        if (!Files.exists(statisticsFile)) return Instant.now();
        try {
            Map<?, ?> saved = yaml.load(Files.readString(statisticsFile));
            if (saved == null) return Instant.now();
            restoreCounter(accepted, saved.get("accepted"));
            restoreCounter(failed, saved.get("failed"));
            restoreBuckets(saved.get("latency-buckets"));
            Object value = saved.get("started-at");
            return value == null ? Instant.now() : Instant.parse(value.toString());
        } catch (Exception exception) {
            logger.warn("Could not load publisher statistics from {}; starting a fresh snapshot",
                    statisticsFile, exception);
            return Instant.now();
        }
    }

    void persistNow() {
        persistSafely();
    }

    private void persistSafely() {
        try {
            Files.createDirectories(statisticsFile.getParent());
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("started-at", startedAt.toString());
            saved.put("updated-at", Instant.now().toString());
            saved.put("accepted", accepted.sum());
            saved.put("failed", failed.sum());
            List<Long> buckets = new ArrayList<>(BUCKET_COUNT);
            for (int i = 0; i < BUCKET_COUNT; i++) buckets.add(latencyBuckets.get(i));
            saved.put("latency-buckets", buckets);
            Path temporary = Files.createTempFile(statisticsFile.getParent(), ".publisher-statistics-", ".tmp");
            try {
                Files.writeString(temporary, yaml.dump(saved), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, statisticsFile, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, statisticsFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | RuntimeException exception) {
            logger.warn("Could not persist publisher statistics to {}", statisticsFile, exception);
        }
    }

    private void restoreCounter(LongAdder counter, Object value) {
        if (value instanceof Number number && number.longValue() >= 0) counter.add(number.longValue());
    }

    private void restoreBuckets(Object value) {
        if (!(value instanceof List<?> values)) return;
        for (int i = 0; i < Math.min(values.size(), BUCKET_COUNT); i++) {
            if (values.get(i) instanceof Number number && number.longValue() >= 0) {
                latencyBuckets.set(i, number.longValue());
            }
        }
    }

    private Double percentile(long samples, double fraction) {
        if (samples == 0) return null;
        long wanted = (long) Math.ceil(samples * fraction);
        long seen = 0;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            seen += latencyBuckets.get(i);
            if (seen >= wanted) return Math.pow(1.01, i + 1) / 1_000_000;
        }
        return null;
    }
}
