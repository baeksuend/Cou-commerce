package com.backsuend.coucommerce.common.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;

@DisplayName("RateLimitingFilter 통합 테스트")
public class RateLimitingFilterIntegrationTest extends BaseIntegrationTest {

	private final String REGISTER_URI = "/api/v1/auth/register";
	@Autowired
	private StringRedisTemplate redisTemplate;

	@AfterEach
	void tearDown() {
		// 테스트 후 Redis 키 삭제
		// MockMvc는 기본적으로 127.0.0.1 IP를 사용합니다.
		String RATE_LIMIT_KEY = "rate_limit:127.0.0.1:" + REGISTER_URI;
		redisTemplate.delete(RATE_LIMIT_KEY);
	}

	@Test
	@DisplayName("단기간에 허용된 요청 횟수를 초과하면 429 Too Many Requests를 반환한다")
	void rateLimit_shouldReturn429_whenRequestsExceedLimit() throws Exception {
		// Given
		SignupRequest signupRequest = new SignupRequest(
			"ratelimit@example.com",
			"password123!",
			"Rate Limiter",
			"010-0000-0000",
			"12345",
			"Test Road",
			"101"
		);
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		int maxRequests = 5; // RateLimitingFilter에 하드코딩된 값

		// When & Then
		// 1. 첫 번째 요청은 성공 (201 Created)
		mockMvc.perform(post(REGISTER_URI)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated());

		// 2. 제한 횟수까지의 나머지 요청은 필터를 통과 (409 Conflict)
		for (int i = 1; i < maxRequests; i++) {
			mockMvc.perform(post(REGISTER_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isConflict());
		}

		// 3. 제한 횟수를 초과한 요청은 거부 (429 Too Many Requests)
		mockMvc.perform(post(REGISTER_URI)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isTooManyRequests()); // 429
	}
}
