package com.backsuend.coucommerce.common.config;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class LoggingFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		try {
			// traceId 생성 및 MDC에 저장
			String traceId = UUID.randomUUID().toString();
			MDC.put("traceId", traceId);

			// memberId는 인증 정보에서 추출 (예시: SecurityContext 사용)
			if (request instanceof HttpServletRequest httpRequest) {
				String memberId = extractMemberId(httpRequest);
				if (memberId != null) {
					MDC.put("memberId", memberId);
				}
			}

			chain.doFilter(request, response);
		} finally {
			MDC.clear(); // 요청 완료 시 MDC 초기화
		}
	}

	private String extractMemberId(HttpServletRequest request) {
		// 예시: JWT나 세션에서 추출
		// 실제 구현 시 Spring Security 또는 custom 인증 방식에 맞게 작성
		return request.getHeader("X-MEMBER-ID"); // 예: 임시 헤더에서 추출
	}
}