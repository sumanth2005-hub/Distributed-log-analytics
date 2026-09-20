package com.suite.payment.controller;

import com.suite.payment.model.PaymentStatus;
import com.suite.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/status/{userId}")
    public PaymentStatus getStatus(@PathVariable String userId) {
        return paymentService.checkStatus(userId);
    }
}