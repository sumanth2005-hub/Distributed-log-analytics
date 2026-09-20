package com.suite.movies.model;

public class Movie {
    private String id;
    private String title;
    private String genre;

    public Movie() {}

    public Movie(String id, String title, String genre) {
        this.id = id;
        this.title = title;
        this.genre = genre;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
}