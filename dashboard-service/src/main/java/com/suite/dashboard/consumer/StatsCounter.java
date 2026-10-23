package com.suite.dashboard.consumer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatsCounter {

    private static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>();

    public StatsCounter(StringRedisTemplate redis) {
        this.redis = redis;
        script.setLocation(new ClassPathResource("scripts/count.lua"));
        script.setResultType(Long.class);
    }

    /** @return true if counted, false if duplicate replay */
    public boolean count(String topic, int partition, long offset,
                         long eventTimeMillis, String service, String level) {
        String minuteKey = "stats:min:" + MINUTE.format(Instant.ofEpochMilli(eventTimeMillis));
        Long result = redis.execute(script,
                List.of("stats:offsets", "stats:total", minuteKey),
                topic + ":" + partition,
                String.valueOf(offset),
                service + ":" + level);
        return result != null && result == 1L;
    }
}