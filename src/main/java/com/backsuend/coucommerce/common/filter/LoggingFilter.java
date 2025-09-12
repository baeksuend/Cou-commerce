package com.backsuend.coucommerce.common.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingFilter implements Filter {

    @Value("${log.user-hash.secret:}")
    private String userHashSecretProp;

    /**
     * MDC-CONTEXT:
     * - 공통 필드: traceId, memberId(해시), memberRole
     * - 접근 로그 필드: http.method, http.path, http.status, latency_ms
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        long startNs = System.nanoTime();
        HttpServletRequest httpReq = (request instanceof HttpServletRequest) ? (HttpServletRequest) request : null;
        HttpServletResponse httpRes = (response instanceof HttpServletResponse) ? (HttpServletResponse) response : null;

        try {
            // 1) traceId 생성
            String traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);

            // 2) 인증 정보 → memberId(해시), memberRole
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                String principal = authentication.getName();
                String hashed = computeMemberIdHash(principal);
                if (hashed != null) {
                    MDC.put("memberId", hashed);
                }
                authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .ifPresent(role -> MDC.put("memberRole", role));
            }

            chain.doFilter(request, response);
        } finally {
            // 3) 접근 로그(Access Log): method/path/status/latency
            try {
                if (httpReq != null) {
                    MDC.put("http.method", httpReq.getMethod());
                    MDC.put("http.path", httpReq.getRequestURI());
                }
                if (httpRes != null) {
                    MDC.put("http.status", String.valueOf(httpRes.getStatus()));
                }
                long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                MDC.put("latency_ms", String.valueOf(latencyMs));
                log.info("access");
            } catch (Exception ignore) {
                // 접근 로그는 베스트에포트
            } finally {
                MDC.clear();
            }
        }
    }

    private String computeMemberIdHash(String principal) {
        if (principal == null || principal.isBlank()) return null;
        String secret = (userHashSecretProp != null && !userHashSecretProp.isBlank())
            ? userHashSecretProp
            : System.getenv("LOG_USER_HASH_SECRET");

        if (secret == null || secret.isBlank()) {
            // 시크릿이 없으면 PII 보호를 위해 memberId를 기록하지 않음
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(principal.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            return null; // 해시 실패 시 기록 회피
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
