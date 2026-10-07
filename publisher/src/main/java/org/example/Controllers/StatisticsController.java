package org.example.Controllers;

import org.example.statistics.PublisherRequestMetrics;
import org.example.statistics.PublisherStatisticsDto;
import org.example.statistics.QueueStatisticsDto;
import org.example.statistics.QueueStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final QueueStatisticsService statisticsService;
    private final PublisherRequestMetrics publisherRequestMetrics;

    public StatisticsController(
            QueueStatisticsService statisticsService,
            PublisherRequestMetrics publisherRequestMetrics) {
        this.statisticsService = statisticsService;
        this.publisherRequestMetrics = publisherRequestMetrics;
    }

    @GetMapping("/queue")
    public QueueStatisticsDto queueStatistics() {
        return statisticsService.getQueueStatistics();
    }

    @GetMapping("/publisher")
    public PublisherStatisticsDto publisherStatistics() {
        return publisherRequestMetrics.snapshot();
    }
}
