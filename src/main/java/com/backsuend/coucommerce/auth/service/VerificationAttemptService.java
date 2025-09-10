package com.backsuend.coucommerce.auth.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationAttemptService {

	private static final String ATTEMPT_PREFIX = "email_verify_attempt:";
	private static final String LOCKOUT_PREFIX = "email_verify_lockout:";

	// 실패 횟수에 따른 잠금 시간 (초 단위)
	private static final long LEVEL_1_ATTEMPTS = 5;
	private static final long LEVEL_1_LOCKOUT_SECONDS = 300; // 5분

	private static final long LEVEL_2_ATTEMPTS = 10;
	private static final long LEVEL_2_LOCKOUT_SECONDS = 600; // 10분

	private static final long LEVEL_3_ATTEMPTS = 15;
	private static final long LEVEL_3_LOCKOUT_SECONDS = 1800; // 30분

	private static final long LEVEL_4_ATTEMPTS = 20;
	private static final long LEVEL_4_LOCKOUT_SECONDS = -1; // 영구 정지 (실제로는 매우 긴 시간으로 설정)

	private final StringRedisTemplate redisTemplate;

	/**
	 * 인증 실패 시 호출되어 실패 횟수를 증가시키고, 임계값 도달 시 계정을 잠급니다.
	 * @param key 이메일 주소 등 고유 식별자
	 */
	public void handleFailedAttempt(String key) {
		String attemptKey = ATTEMPT_PREFIX + key;
		long attempts = redisTemplate.opsForValue().increment(attemptKey, 1);

		// 최초 실패 시, 실패 기록이 1시간 뒤에 사라지도록 설정 (계속 쌓이는 것을 방지)
		if (attempts == 1) {
			redisTemplate.expire(attemptKey, 1, TimeUnit.HOURS);
		}

		long lockoutSeconds = getLockoutSeconds(attempts);
		if (lockoutSeconds != 0) {
			String lockoutKey = LOCKOUT_PREFIX + key;
			if (lockoutSeconds == -1) {
				// 영구 정지 (실제로는 100년으로 설정)
				redisTemplate.opsForValue().set(lockoutKey, String.valueOf(attempts), 100 * 365, TimeUnit.DAYS);
			} else {
				redisTemplate.opsForValue().set(lockoutKey, String.valueOf(attempts), lockoutSeconds, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * 인증 성공 시 호출되어 모든 실패 기록과 잠금 상태를 초기화합니다.
	 * @param key 이메일 주소 등 고유 식별자
	 */
	public void resetAttempts(String key) {
		redisTemplate.delete(ATTEMPT_PREFIX + key);
		redisTemplate.delete(LOCKOUT_PREFIX + key);
	}

	/**
	 * 해당 키(이메일)가 현재 잠금 상태인지 확인합니다.
	 * @param key 이메일 주소 등 고유 식별자
	 * @return 잠금 상태 여부
	 */
	public boolean isBlocked(String key) {
		return redisTemplate.hasKey(LOCKOUT_PREFIX + key);
	}

	private long getLockoutSeconds(long attempts) {
		if (attempts == LEVEL_4_ATTEMPTS) {
			return LEVEL_4_LOCKOUT_SECONDS;
		}
		if (attempts == LEVEL_3_ATTEMPTS) {
			return LEVEL_3_LOCKOUT_SECONDS;
		}
		if (attempts == LEVEL_2_ATTEMPTS) {
			return LEVEL_2_LOCKOUT_SECONDS;
		}
		if (attempts == LEVEL_1_ATTEMPTS) {
			return LEVEL_1_LOCKOUT_SECONDS;
		}
		return 0;
	}
}
