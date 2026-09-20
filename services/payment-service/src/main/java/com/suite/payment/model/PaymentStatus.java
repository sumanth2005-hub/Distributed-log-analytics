package com.suite.payment.model;

public class PaymentStatus {
    private String userId;
    private String status;

    public PaymentStatus() {}

    public PaymentStatus(String userId, String status) {
        this.userId = userId;
        this.status = status;
    }

    public String getUserId() { return userId; }
    public String getStatus() { return status; }
}