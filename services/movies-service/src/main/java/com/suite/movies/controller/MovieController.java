package com.suite.movies.controller;

import com.suite.movies.model.Movie;
import com.suite.movies.service.MovieService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/{id}")
    public Movie getMovie(@PathVariable String id) {
        return movieService.getMovie(id);
    }
}