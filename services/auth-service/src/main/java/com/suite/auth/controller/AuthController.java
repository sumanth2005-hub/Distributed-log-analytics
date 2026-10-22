package com.suite.auth.controller;

import com.suite.auth.model.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String SERVICE_NAME = "auth-service";
    private static final String TOPIC = "service-logs";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final int failPercent;

    public AuthController(KafkaTemplate<String, Object> kafkaTemplate,
                          RestTemplate restTemplate,
                          @Value("${auth.fail-percent:0}") int failPercent) {
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.failPercent = failPercent;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String userId, @RequestParam String password) {
        long start = System.currentTimeMillis();

        if (ThreadLocalRandom.current().nextInt(100) < failPercent) {
            publish("ERROR", "login failed for user=" + userId + " - invalid credentials", start);
            return ResponseEntity.status(401).body("Login failed");
        }

        try {
            String profile = restTemplate.getForObject(
                    "http://user-service:8082/api/users/" + userId, String.class);
            publish("INFO", "login request for user=" + userId, start);
            return ResponseEntity.ok("token-for-" + userId + " | profile=" + profile);
        } catch (RuntimeException e) {
            publish("ERROR", "login failed for user=" + userId + ": " + e.getMessage(), start);
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