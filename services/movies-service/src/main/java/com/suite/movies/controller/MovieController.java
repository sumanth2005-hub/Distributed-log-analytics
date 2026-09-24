package com.suite.movies.controller;

import com.suite.movies.model.LogEvent;
import com.suite.movies.model.Movie;
import com.suite.movies.service.MovieService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MovieController(MovieService movieService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.movieService = movieService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/{id}")
    public Movie getMovie(@PathVariable String id) {
        long start = System.currentTimeMillis();
        Movie result = movieService.getMovie(id);
        long latency = System.currentTimeMillis() - start;

        kafkaTemplate.send("service-logs",
                new LogEvent("movies-service", "INFO", "getMovie request for id=" + id, System.currentTimeMillis(), latency));

        return result;
    }
}