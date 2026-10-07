package org.example.Controllers;

import org.example.statistics.QueueStatisticsDto;
import org.example.statistics.QueueStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final QueueStatisticsService statisticsService;

    public StatisticsController(QueueStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/queue")
    public QueueStatisticsDto queueStatistics() {
        return statisticsService.getQueueStatistics();
    }
}
