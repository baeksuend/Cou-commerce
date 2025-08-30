package com.backsuend.coucommerce.common.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.service.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

	private static final List<String> AUTH_ENDPOINTS = Arrays.asList(
		"/api/v1/auth/login",
		"/api/v1/auth/register"
	);
	private static final int MAX_REQUESTS = 5; // 예: 1분당 5회 요청
	private static final int WINDOW_SECONDS = 60; // 윈도우 시간 (초)
	private final RateLimitingService rateLimitingService;
	private final ObjectMapper objectMapper;

	@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		String requestUri = request.getRequestURI();

		if (AUTH_ENDPOINTS.contains(requestUri)) {
			String clientIp = getClientIp(request);
			String rateLimitKey = clientIp + ":" + requestUri;

			if (!rateLimitingService.allowRequest(rateLimitKey, MAX_REQUESTS, WINDOW_SECONDS)) {
				log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
				sendTooManyRequestsResponse(response);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null || !xfHeader.contains(".")) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}

	private void sendTooManyRequestsResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Object> apiResponse = ApiResponse.error(
			ErrorCode.TOO_MANY_REQUESTS.status().value(),
			"요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."
		);
		String jsonResponse = objectMapper.writeValueAsString(apiResponse);
		response.getWriter().write(jsonResponse);
	}
}
