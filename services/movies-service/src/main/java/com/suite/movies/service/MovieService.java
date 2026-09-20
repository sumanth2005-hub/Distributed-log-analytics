package com.suite.movies.service;

import com.suite.movies.model.Movie;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MovieService {

    private final Map<String, Movie> store = new ConcurrentHashMap<>();

    public MovieService() {
        store.put("m1", new Movie("m1", "Spider-Verse", "Animation"));
    }

    public Movie getMovie(String id) {
        return store.getOrDefault(id, new Movie(id, "Unknown", "Unknown"));
    }
}