package com.suite.dashboard.consumer;

import com.suite.dashboard.model.LogEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LogConsumer {

    // temp in-memory store for dashboard log feed panel — swap for Redis/DB later
    private final List<LogEvent> recentLogs = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "service-logs", groupId = "log-consumer")
    public void consume(LogEvent event) {
        System.out.println("Consumed: " + event.getService() + " | " + event.getLevel() + " | " + event.getMessage() + " | " + event.getLatencyMs() + "ms");
        recentLogs.add(0, event);
        if (recentLogs.size() > 100) {
            recentLogs.remove(recentLogs.size() - 1);
        }
    }

    public List<LogEvent> getRecentLogs() {
        return Collections.unmodifiableList(recentLogs);
    }
}