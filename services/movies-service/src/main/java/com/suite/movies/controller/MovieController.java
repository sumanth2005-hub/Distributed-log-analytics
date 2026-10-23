package com.suite.movies.controller;

import com.suite.movies.model.LogEvent;
import com.suite.movies.model.Movie;
import com.suite.movies.service.MovieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private static final String SERVICE_NAME = "movies-service";
    private static final String TOPIC = "service-logs";

    private final MovieService movieService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final int failPercent;

    public MovieController(MovieService movieService,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           @Value("${movies.fail-percent:0}") int failPercent) {
        this.movieService = movieService;
        this.kafkaTemplate = kafkaTemplate;
        this.failPercent = failPercent;
    }

    @GetMapping("/{id}")
    public Movie getMovie(@PathVariable String id) {
        long start = System.currentTimeMillis();
        try {
            if (ThreadLocalRandom.current().nextInt(100) < failPercent) {
                throw new IllegalStateException("movie catalog unavailable");
            }
            Movie result = movieService.getMovie(id);
            publish("INFO", "getMovie request for id=" + id, start);
            return result;
        } catch (RuntimeException e) {
            publish("ERROR", "getMovie failed for id=" + id + ": " + e.getMessage(), start);
            throw e;
        }
    }

    private void publish(String level, String message, long start) {
        try {
            long now = System.currentTimeMillis();
            kafkaTemplate.send(TOPIC, SERVICE_NAME,
                    new LogEvent(SERVICE_NAME, level, message, now, now - start));
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger("kafka-publish").warn("publish failed", ignored);
            // never break the request because logging failed
        }
    }
}