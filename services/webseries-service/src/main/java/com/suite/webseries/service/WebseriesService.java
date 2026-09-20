package com.suite.webseries.service;

import com.suite.webseries.client.PaymentClient;
import com.suite.webseries.client.UserClient;
import org.springframework.stereotype.Service;

@Service
public class WebseriesService {
    private final UserClient userClient;
    private final PaymentClient paymentClient;

    public WebseriesService(UserClient userClient, PaymentClient paymentClient) {
        this.userClient = userClient;
        this.paymentClient = paymentClient;
    }

    public String watch(String userId, String seriesId) {
        String user = userClient.fetchUser(userId);
        String subStatus = paymentClient.checkSubscription(userId);
        return "playing " + seriesId + " for " + user + " | subscription=" + subStatus;
    }
}