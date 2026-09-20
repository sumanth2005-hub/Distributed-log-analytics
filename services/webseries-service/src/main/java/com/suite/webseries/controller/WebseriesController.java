package com.suite.webseries.controller;

import com.suite.webseries.service.WebseriesService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webseries")
public class WebseriesController {
    private final WebseriesService webseriesService;

    public WebseriesController(WebseriesService webseriesService) {
        this.webseriesService = webseriesService;
    }

    @GetMapping("/watch")
    public String watch(@RequestParam String userId, @RequestParam String seriesId) {
        return webseriesService.watch(userId, seriesId);
    }
}