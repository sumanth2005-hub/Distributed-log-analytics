package com.suite.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserClient(RestTemplate restTemplate,
                      @Value("${user.service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public String fetchUserProfile(String userId) {
        return restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, String.class);
    }
}