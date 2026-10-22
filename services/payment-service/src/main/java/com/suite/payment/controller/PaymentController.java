package com.suite.payment.controller;

import com.suite.payment.model.LogEvent;
import com.suite.payment.model.PaymentStatus;
import com.suite.payment.service.PaymentService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final String SERVICE_NAME = "payment-service";
    private static final String TOPIC = "service-logs";

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AtomicBoolean chaos = new AtomicBoolean(false);

    public PaymentController(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/status/{userId}")
    public PaymentStatus getStatus(@PathVariable String userId) {
        long start = System.currentTimeMillis();
        try {
            if (chaos.get()) {
                throw new IllegalStateException("chaos mode: payment gateway unavailable");
            }
            PaymentStatus result = paymentService.checkStatus(userId);
            publish("INFO", "status check for userId=" + userId, start);
            return result;
        } catch (RuntimeException e) {
            publish("ERROR", "status check failed for userId=" + userId + ": " + e.getMessage(), start);
            throw e;
        }
    }

    @PostMapping("/chaos/fail")
    public Map<String, Object> chaosOn() {
        chaos.set(true);
        return Map.of("service", SERVICE_NAME, "chaos", true);
    }

    @PostMapping("/chaos/reset")
    public Map<String, Object> chaosOff() {
        chaos.set(false);
        return Map.of("service", SERVICE_NAME, "chaos", false);
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