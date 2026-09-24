package com.suite.dashboard.controller;

import com.suite.dashboard.consumer.LogConsumer;
import com.suite.dashboard.model.LogEvent;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogConsumer logConsumer;

    public LogController(LogConsumer logConsumer) {
        this.logConsumer = logConsumer;
    }

    @GetMapping("/recent")
    public List<LogEvent> getRecentLogs() {
        return logConsumer.getRecentLogs();
    }
}