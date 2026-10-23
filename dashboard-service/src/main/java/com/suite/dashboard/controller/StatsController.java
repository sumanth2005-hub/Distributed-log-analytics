package com.suite.dashboard.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class StatsController {

    private static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;

    public StatsController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/api/stats")
    public Map<String, Object> stats(@RequestParam(defaultValue = "30") int minutes) {
        int window = Math.max(1, Math.min(minutes, 120));

        // totals: service -> level -> count
        Map<String, Map<String, Long>> totals = new TreeMap<>();
        for (Map.Entry<Object, Object> e : redis.opsForHash().entries("stats:total").entrySet()) {
            String field = e.getKey().toString();
            int idx = field.lastIndexOf(':');
            if (idx < 0) continue;
            totals.computeIfAbsent(field.substring(0, idx), k -> new TreeMap<>())
                    .merge(field.substring(idx + 1), Long.parseLong(e.getValue().toString()), Long::sum);
        }

        String highest = null;
        long highestCount = -1;
        for (Map.Entry<String, Map<String, Long>> s : totals.entrySet()) {
            long sum = s.getValue().values().stream().mapToLong(Long::longValue).sum();
            if (sum > highestCount) {
                highestCount = sum;
                highest = s.getKey();
            }
        }

        // per minute: oldest -> newest, one bucket per minute
        List<Map<String, Object>> perMinute = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = window - 1; i >= 0; i--) {
            String minute = MINUTE.format(Instant.ofEpochMilli(now - i * 60_000L));
            long info = 0;
            long error = 0;
            for (Map.Entry<Object, Object> e : redis.opsForHash().entries("stats:min:" + minute).entrySet()) {
                long v = Long.parseLong(e.getValue().toString());
                if (e.getKey().toString().endsWith(":ERROR")) error += v;
                else info += v;
            }
            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("minute", minute);
            bucket.put("info", info);
            bucket.put("error", error);
            perMinute.add(bucket);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totals", totals);
        out.put("highestVolume", highest);
        out.put("perMinute", perMinute);
        return out;
    }
}