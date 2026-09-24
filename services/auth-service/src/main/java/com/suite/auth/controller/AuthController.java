package com.suite.auth.controller;

import com.suite.auth.model.LogEvent;
import com.suite.auth.service.AuthService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RestTemplate restTemplate;

    private final Random random = new Random();

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String userId, @RequestParam String password) {
        long start = System.currentTimeMillis();

        // 30% simulated failure
        boolean simulateError = random.nextInt(100) < 30;

        if (simulateError) {
            long latency = System.currentTimeMillis() - start;
            kafkaTemplate.send("service-logs",
                    new LogEvent("auth-service", "ERROR",
                            "login failed for user=" + userId + " — invalid credentials",
                            System.currentTimeMillis(), latency));
            return ResponseEntity.status(401).body("Login failed");
        }

        // existing success path
        String profile = restTemplate.getForObject(
                "http://user-service:8082/api/users/" + userId, String.class);

        long latency = System.currentTimeMillis() - start;
        kafkaTemplate.send("service-logs",
                new LogEvent("auth-service", "INFO",
                        "login request for user=" + userId, System.currentTimeMillis(), latency));

        return ResponseEntity.ok("token-for-" + userId + " | profile=" + profile);
    }
}