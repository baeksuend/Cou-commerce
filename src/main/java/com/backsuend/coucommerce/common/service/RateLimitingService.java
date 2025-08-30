package com.backsuend.coucommerce.common.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

	private final RedisTemplate<String, String> redisTemplate;

	/**
	 * 지정된 키에 대한 요청 속도를 확인하고 제한합니다.
	 *
	 * @param key 속도 제한을 적용할 고유 키 (예: IP 주소, 사용자 ID + 엔드포인트)
	 * @param maxRequests 허용되는 최대 요청 수
	 * @param windowSeconds 속도 제한 윈도우 (초)
	 * @return 요청이 허용되면 true, 속도 제한에 걸리면 false
	 */
	public boolean allowRequest(String key, int maxRequests, int windowSeconds) {
		String countKey = "rate_limit:" + key;
		Long currentCount = redisTemplate.opsForValue().increment(countKey);

		if (currentCount == null) {
			currentCount = 1L;
		}

		if (currentCount == 1) {
			// 윈도우의 첫 번째 요청이면 만료 시간 설정
			redisTemplate.expire(countKey, Duration.ofSeconds(windowSeconds));
		}

		return currentCount <= maxRequests;
	}
}
