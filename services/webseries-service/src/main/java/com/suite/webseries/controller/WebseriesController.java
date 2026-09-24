package com.suite.webseries.controller;

import com.suite.webseries.model.LogEvent;
import com.suite.webseries.service.WebseriesService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webseries")
public class WebseriesController {
    private final WebseriesService webseriesService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WebseriesController(WebseriesService webseriesService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.webseriesService = webseriesService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/watch")
    public String watch(@RequestParam String userId, @RequestParam String seriesId) {
        long start = System.currentTimeMillis();
        String result = webseriesService.watch(userId, seriesId);
        long latency = System.currentTimeMillis() - start;

        kafkaTemplate.send("service-logs",
                new LogEvent("webseries-service", "INFO", "watch request served for user=" + userId, System.currentTimeMillis(), latency));

        return result;
    }
}