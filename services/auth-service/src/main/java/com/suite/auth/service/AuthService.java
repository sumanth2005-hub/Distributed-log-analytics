package com.suite.auth.service;

import com.suite.auth.client.UserClient;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserClient userClient;

    public AuthService(UserClient userClient) {
        this.userClient = userClient;
    }

    public String login(String userId, String password) {
        // mock auth check — replace with real JWT logic later
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId required");
        }
        String userProfile = userClient.fetchUserProfile(userId);
        return "token-for-" + userId + " | profile=" + userProfile;
    }
}