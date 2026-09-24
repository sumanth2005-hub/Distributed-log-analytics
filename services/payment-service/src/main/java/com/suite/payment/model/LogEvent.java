package com.suite.payment.model;

public class LogEvent {
    private String service;
    private String level;
    private String message;
    private long timestamp;
    private long latencyMs;

    public LogEvent() {}

    public LogEvent(String service, String level, String message, long timestamp, long latencyMs) {
        this.service = service;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
        this.latencyMs = latencyMs;
    }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}