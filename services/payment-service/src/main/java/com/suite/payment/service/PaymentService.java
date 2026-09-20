package com.suite.payment.service;

import com.suite.payment.model.PaymentStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    public PaymentStatus checkStatus(String userId) {
        return new PaymentStatus(userId, "ACTIVE");
    }
}