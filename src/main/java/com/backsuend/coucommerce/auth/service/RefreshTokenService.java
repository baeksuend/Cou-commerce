package com.backsuend.coucommerce.auth.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.jwt.JwtProvider;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.service.MdcLogging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

	private static final String REFRESH_TOKEN_PREFIX = "refreshToken:";
	private static final String USER_TOKENS_PREFIX = "user-tokens:";

	private final StringRedisTemplate redisTemplate;
	private final JwtProvider jwtProvider;
	private final ObjectMapper objectMapper;

	@Value("${jwt.refresh-token-expiration-time}")
	private long refreshTokenExpirationTime;

	private String getUserTokensKey(Long userId) {
		return USER_TOKENS_PREFIX + userId;
	}

	private String getRefreshTokenKey(String token) {
		return REFRESH_TOKEN_PREFIX + token;
	}

	/**
	 * 새로운 Refresh Token을 생성하고 Redis에 저장합니다.
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - userId: 사용자 숫자 ID
	 */
	public String createRefreshToken(String email, Long userId) {
		try (var ignored = MdcLogging.withContext("userId", String.valueOf(userId))) {
			String token = jwtProvider.createRefreshToken(email);
			RefreshTokenInfo info = new RefreshTokenInfo(email, userId, false);
			String redisKey = getRefreshTokenKey(token);
			String userTokensKey = getUserTokensKey(userId);

			try {
				String infoJson = objectMapper.writeValueAsString(info);
				redisTemplate.opsForValue().set(redisKey, infoJson, Duration.ofMillis(refreshTokenExpirationTime));
				redisTemplate.opsForSet().add(userTokensKey, token);
				redisTemplate.expire(userTokensKey, Duration.ofMillis(refreshTokenExpirationTime));
				log.info("사용자 {}를 위해 Redis에 리프레시 토큰을 생성하고 저장했습니다: {}", userId, redisKey);
			} catch (JsonProcessingException e) {
				log.error("Redis를 위한 RefreshTokenInfo 직렬화에 실패했습니다: {}", e.getMessage());
				throw new BusinessException(ErrorCode.INTERNAL_ERROR, "리프레시 토큰 저장 중 오류가 발생했습니다.");
			}
			return token;
		}
	}

	private Optional<RefreshTokenInfo> readTokenInfo(String token) {
		String redisKey = getRefreshTokenKey(token);
		String infoJson = redisTemplate.opsForValue().get(redisKey);
		if (infoJson == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(infoJson, RefreshTokenInfo.class));
		} catch (JsonProcessingException e) {
			log.error("Redis에서 토큰 {}에 대한 RefreshTokenInfo 역직렬화에 실패했습니다: {}", token, e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "리프레시 토큰 정보 조회 중 오류가 발생했습니다.");
		}
	}

	public Optional<RefreshTokenInfo> findByToken(String token) {
		Optional<RefreshTokenInfo> tokenInfoOpt = readTokenInfo(token);
		if (tokenInfoOpt.isEmpty()) {
			return Optional.empty();
		}

		RefreshTokenInfo info = tokenInfoOpt.get();
		if (info.used()) {
			log.warn("사용자 {}의 리프레시 토큰 재사용이 감지되었습니다. 모든 세션을 종료합니다.", info.userId());
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 이미 사용되었습니다. 모든 세션이 종료됩니다.");
		}

		try {
			RefreshTokenInfo usedInfo = new RefreshTokenInfo(info.email(), info.userId(), true);
			String redisKey = getRefreshTokenKey(token);
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(usedInfo), Duration.ofSeconds(5));
		} catch (JsonProcessingException e) {
			log.error("Redis에 사용된 RefreshTokenInfo를 직렬화하는 데 실패했습니다: {}", e.getMessage());
			// Continue without throwing, as the main goal is to return the token info
		}

		return Optional.of(info);
	}

	@Transactional
	public void deleteByToken(String token) {
		String redisKey = getRefreshTokenKey(token);
		readTokenInfo(token).ifPresent(info -> {
			String userTokensKey = getUserTokensKey(info.userId());
			redisTemplate.opsForSet().remove(userTokensKey, token);
		});
		Boolean deleted = redisTemplate.delete(redisKey);
		if (deleted) {
			log.info("Redis에서 리프레시 토큰을 삭제했습니다: {}", redisKey);
		} else {
			log.warn("Redis에 존재하지 않는 리프레시 토큰 삭제 시도: {}", redisKey);
		}
	}

	/**
	 * 특정 사용자의 모든 Refresh Token을 무효화합니다.
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - userId: 사용자 숫자 ID
	 */
	@Transactional
	public void deleteAllTokensForUser(Long userId) {
		try (var ignored = MdcLogging.withContext("userId", String.valueOf(userId))) {
			String userTokensKey = getUserTokensKey(userId);
			Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
			if (tokens == null || tokens.isEmpty()) {
				return;
			}
			List<String> tokenKeys = tokens.stream()
				.map(this::getRefreshTokenKey)
				.collect(Collectors.toList());
			redisTemplate.delete(tokenKeys);
			redisTemplate.delete(userTokensKey);
			log.info("사용자 {}의 모든 리프레시 토큰이 무효화되었습니다.", userId);
		}
	}

	public record RefreshTokenInfo(String email, Long userId, boolean used) {
	}
}
