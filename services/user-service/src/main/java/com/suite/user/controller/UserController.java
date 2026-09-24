package com.suite.user.controller;

import com.suite.user.model.LogEvent;
import com.suite.user.model.User;
import com.suite.user.service.UserService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserController(UserService userService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userService = userService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        long start = System.currentTimeMillis();
        User result = userService.getUser(id);
        long latency = System.currentTimeMillis() - start;

        kafkaTemplate.send("service-logs",
                new LogEvent("user-service", "INFO", "getUser request for id=" + id, System.currentTimeMillis(), latency));

        return result;
    }
}