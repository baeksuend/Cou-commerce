package com.backsuend.coucommerce.auth.jwt;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException)
		throws IOException {
		log.warn("사용자 접근 거부: {}, 경로: {}", request.getRemoteUser(), request.getRequestURI());

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.ACCESS_DENIED.status().value(), "접근 권한이 없습니다.");
		String jsonResponse = objectMapper.writeValueAsString(apiResponse);

		response.getWriter().write(jsonResponse);
	}
}
