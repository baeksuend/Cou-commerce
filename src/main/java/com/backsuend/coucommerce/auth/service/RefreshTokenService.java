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

	public String createRefreshToken(String email, Long userId) {
		String token = jwtProvider.createRefreshToken(email);
		RefreshTokenInfo info = new RefreshTokenInfo(email, userId, false);
		String redisKey = getRefreshTokenKey(token);
		String userTokensKey = getUserTokensKey(userId);

		try {
			String infoJson = objectMapper.writeValueAsString(info);
			redisTemplate.opsForValue().set(redisKey, infoJson, Duration.ofMillis(refreshTokenExpirationTime));
			redisTemplate.opsForSet().add(userTokensKey, token);
			redisTemplate.expire(userTokensKey, Duration.ofMillis(refreshTokenExpirationTime));
			log.info("Refresh token created and stored in Redis for user {}: {}", userId, redisKey);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize RefreshTokenInfo for Redis: {}", e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "리프레시 토큰 저장 중 오류가 발생했습니다.");
		}
		return token;
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
			log.error("Failed to deserialize RefreshTokenInfo from Redis for token {}: {}", token, e.getMessage());
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
			log.warn("Detected refresh token reuse for user {}. Invalidating all tokens.", info.userId());
			deleteAllTokensForUser(info.userId());
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 이미 사용되었습니다. 모든 세션이 종료됩니다.");
		}

		try {
			RefreshTokenInfo usedInfo = new RefreshTokenInfo(info.email(), info.userId(), true);
			String redisKey = getRefreshTokenKey(token);
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(usedInfo), Duration.ofSeconds(5));
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize used RefreshTokenInfo for Redis: {}", e.getMessage());
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
			log.info("Refresh token deleted from Redis: {}", redisKey);
		} else {
			log.warn("Attempted to delete non-existent refresh token from Redis: {}", redisKey);
		}
	}

	@Transactional
	public void deleteAllTokensForUser(Long userId) {
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
		log.info("All refresh tokens for user {} have been invalidated.", userId);
	}

	public record RefreshTokenInfo(String email, Long userId, boolean used) {
	}
}
