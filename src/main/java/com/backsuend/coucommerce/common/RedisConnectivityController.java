package com.backsuend.coucommerce.common;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rua
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RedisConnectivityController {

	private final StringRedisTemplate redis;

	/**
	 * Redis 연결 상태를 확인합니다. (시스템 로그)
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 */
	@GetMapping("/redis/ping")
	public Map<String, Object> ping() {
		try {
			Map<String, Object> res = new LinkedHashMap<>();

			// 1) PING
			String pong = Objects.requireNonNull(redis.getConnectionFactory()).getConnection().ping();

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

			log.info("Redis 헬스 체크 성공. Ping: {}, Key: {}, Value: {}, TTL: {}", pong, key, val, ttl);
			return res;
		} catch (Exception e) {
			log.error("Redis 헬스 체크 실패: {}", e.getMessage(), e);
			throw new IllegalStateException("Redis 연결에 실패했습니다.");
		}
	}
}
