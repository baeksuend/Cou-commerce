package com.backsuend.coucommerce.auth.jwt;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.backsuend.coucommerce.auth.service.UserDetailsServiceImpl;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT(JSON Web Token)를 사용하여 HTTP 요청을 인증하는 필터.
 * 요청 헤더에서 JWT를 추출하고, 유효성을 검증한 후, 유효한 토큰이라면
 * 사용자 정보를 기반으로 Spring Security Context에 인증 객체를 설정한다.
 * 이를 통해 이후의 요청 처리 과정에서 인증된 사용자 정보를 사용할 수 있게 한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;
	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	)

		throws ServletException, IOException {
		String jwt = parseJwt(request);

		if (jwt != null) {
			try {
				jwtProvider.validateToken(jwt);
				String email = jwtProvider.getEmailFromToken(jwt);

				// DB에서 최신 사용자 정보를 로드하여 토큰 정보의 유효성(예: 계정 잠금, 권한 변경 등)을 확인한다.
				// 이는 토큰이 탈취되거나 사용자 상태가 변경되었을 때 발생할 수 있는 보안 문제를 방지한다.
				UserDetails userDetails = userDetailsService.loadUserByUsername(email);
				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (ExpiredJwtException e) {
				log.warn("Expired JWT token: {}", e.getMessage());
			} catch (JwtException | IllegalArgumentException e) {
				log.warn("Invalid JWT token: {}", e.getMessage());
			}
		}

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");

		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7);
		}

		return null;
	}
}
