package com.suite.payment.controller;

import com.suite.payment.model.LogEvent;
import com.suite.payment.model.PaymentStatus;
import com.suite.payment.service.PaymentService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentController(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/status/{userId}")
    public PaymentStatus getStatus(@PathVariable String userId) {
        long start = System.currentTimeMillis();
        PaymentStatus result = paymentService.checkStatus(userId);
        long latency = System.currentTimeMillis() - start;

        kafkaTemplate.send("service-logs",
                new LogEvent("payment-service", "INFO", "status check for userId=" + userId, System.currentTimeMillis(), latency));

        return result;
    }
}