package com.suite.user.controller;

import com.suite.user.model.LogEvent;
import com.suite.user.model.User;
import com.suite.user.service.UserService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final String SERVICE_NAME = "user-service";
    private static final String TOPIC = "service-logs";

    private final UserService userService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserController(UserService userService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userService = userService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        long start = System.currentTimeMillis();
        try {
            User result = userService.getUser(id);
            publish("INFO", "getUser request for id=" + id, start);
            return result;
        } catch (RuntimeException e) {
            publish("ERROR", "getUser failed for id=" + id + ": " + e.getMessage(), start);
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