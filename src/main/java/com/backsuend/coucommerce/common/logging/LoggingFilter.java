package com.backsuend.coucommerce.common.logging;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author rua
 */

@Component
public class LoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain)
		throws ServletException, IOException {
		try {
			// traceId 생성 (없으면 새로 생성)
			String traceId = UUID.randomUUID().toString();
			MDC.put("traceId", traceId);

			// JWT 기반 memberId 추출 (예시)
			String memberId = request.getHeader("X-Member-Id");
			MDC.put("memberId", memberId != null ? memberId : "guest");

			filterChain.doFilter(request, response);
		} finally {
			MDC.clear(); // 요청 끝나면 반드시 정리
		}
	}
}
