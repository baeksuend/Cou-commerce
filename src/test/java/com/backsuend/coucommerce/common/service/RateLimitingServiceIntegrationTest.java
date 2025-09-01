package com.backsuend.coucommerce.common.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.backsuend.coucommerce.BaseIntegrationTest;

@DisplayName("RateLimitingService 통합 테스트")
public class RateLimitingServiceIntegrationTest extends BaseIntegrationTest {

	private final String TEST_KEY = "test:ip:127.0.0.1";
	@Autowired
	private RateLimitingService rateLimitingService;
	@Autowired
	private StringRedisTemplate redisTemplate;

	@AfterEach
	void tearDown() {
		// 테스트 후 Redis 키 삭제
		redisTemplate.delete("rate_limit:" + TEST_KEY);
	}

	@Test
	@DisplayName("요청이 제한 횟수를 초과하면 false를 반환한다")
	void allowRequest_shouldReturnFalse_whenRequestsExceedLimit() {
		// Given
		int maxRequests = 5;
		int windowSeconds = 10;

		// When & Then (허용)
		for (int i = 0; i < maxRequests; i++) {
			boolean allowed = rateLimitingService.allowRequest(TEST_KEY, maxRequests, windowSeconds);
			assertThat(allowed).isTrue();
		}

		// When & Then (거부)
		boolean denied = rateLimitingService.allowRequest(TEST_KEY, maxRequests, windowSeconds);
		assertThat(denied).isFalse();
	}

	@Test
	@DisplayName("요청이 거부된 후 시간이 지나면 다시 허용된다")
	void allowRequest_shouldReturnTrue_afterWindowExpires() throws InterruptedException {
		// Given
		int maxRequests = 5;
		int windowSeconds = 1; // 테스트 시간을 줄이기 위해 1초로 설정

		// 요청 횟수 초과
		for (int i = 0; i < maxRequests; i++) {
			rateLimitingService.allowRequest(TEST_KEY, maxRequests, windowSeconds);
		}
		boolean denied = rateLimitingService.allowRequest(TEST_KEY, maxRequests, windowSeconds);
		assertThat(denied).isFalse(); // 거부 확인

		// When
		// 윈도우 시간(1초)보다 더 오래 대기
		Thread.sleep(1100);

		// Then
		boolean allowedAgain = rateLimitingService.allowRequest(TEST_KEY, maxRequests, windowSeconds);
		assertThat(allowedAgain).isTrue();
	}
}
