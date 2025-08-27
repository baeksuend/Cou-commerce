package com.backsuend.coucommerce.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author rua
 */
@RestController
public class RedisConnectivityController {

    private final StringRedisTemplate redis;

    public RedisConnectivityController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/redis/ping")
    public Map<String, Object> ping() {
        Map<String, Object> res = new LinkedHashMap<>();

        // 1) PING
        String pong = redis.getConnectionFactory().getConnection().ping();

        // 2) SET with TTL
        String key = "ping:" + UUID.randomUUID();
        ValueOperations<String, String> ops = redis.opsForValue();
        ops.set(key, "pong", Duration.ofSeconds(30));

        // 3) GET + TTL
        String val = ops.get(key);
        Long ttl = redis.getExpire(key); // seconds

        res.put("redisPing", pong);      // 기대값: "PONG"
        res.put("key", key);
        res.put("value", val);           // 기대값: "pong"
        res.put("ttlSeconds", ttl);      // 30 근처 숫자
        return res;
    }
}