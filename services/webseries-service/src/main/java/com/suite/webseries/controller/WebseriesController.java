package com.suite.webseries.controller;

import com.suite.webseries.model.LogEvent;
import com.suite.webseries.service.WebseriesService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webseries")
public class WebseriesController {
    private static final String SERVICE_NAME = "webseries-service";
    private static final String TOPIC = "service-logs";

    private final WebseriesService webseriesService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WebseriesController(WebseriesService webseriesService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.webseriesService = webseriesService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/watch")
    public String watch(@RequestParam String userId, @RequestParam String seriesId) {
        long start = System.currentTimeMillis();
        try {
            String result = webseriesService.watch(userId, seriesId);
            publish("INFO", "watch request served for user=" + userId, start);
            return result;
        } catch (RuntimeException e) {
            publish("ERROR", "watch failed for user=" + userId + ": " + e.getMessage(), start);
            throw e;
        }
    }

    private void publish(String level, String message, long start) {
        try {
            long now = System.currentTimeMillis();
            kafkaTemplate.send(TOPIC, SERVICE_NAME,
                    new LogEvent(SERVICE_NAME, level, message, now, now - start));
        } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger("kafka-publish").warn("publish failed", ignored);
            // never break the request because logging failed
        }
    }
}