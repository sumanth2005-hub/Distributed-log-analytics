package com.suite.user.service;

import com.suite.user.model.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    // mock in-memory store — swap for DB later
    private final Map<String, User> store = new ConcurrentHashMap<>();

    public UserService() {
        store.put("u1", new User("u1", "Miles", "miles@example.com"));
    }

    public User getUser(String id) {
        return store.getOrDefault(id, new User(id, "Unknown", "unknown@example.com"));
    }
}