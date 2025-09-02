package com.backsuend.coucommerce.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.backsuend.coucommerce.auth.jwt.JwtAccessDeniedHandler;
import com.backsuend.coucommerce.auth.jwt.JwtAuthenticationEntryPoint;
import com.backsuend.coucommerce.auth.jwt.JwtAuthenticationFilter;
import com.backsuend.coucommerce.auth.jwt.JwtProvider;
import com.backsuend.coucommerce.auth.service.UserDetailsServiceImpl;
import com.backsuend.coucommerce.common.filter.RateLimitingFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] WHITE_LIST = {
		"/api/v1/auth/**",    // Auth API
		"/api/v1/products/**",    // Products API
		"/api/v1/seller/**",    // Seller API
		"/swagger-ui/**",     // Swagger UI
		"/v3/api-docs/**",    // Swagger API 문서
		"/api/v1/redis/ping"  // Redis 연결 테스트
	}; // 인증 없이 접근을 허용하는 공개(Public) URL 패턴 목록
	private final JwtProvider jwtProvider;
	private final UserDetailsServiceImpl userDetailsService;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final RateLimitingFilter rateLimitingFilter;

	@Value("${cors.allowed-origins}") // Added annotation
	private String[] allowedOrigins; // Added field

	public SecurityConfig(JwtProvider jwtProvider,
		UserDetailsServiceImpl userDetailsService,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		JwtAccessDeniedHandler jwtAccessDeniedHandler,
		RateLimitingFilter rateLimitingFilter) {
		this.jwtProvider = jwtProvider;
		this.userDetailsService = userDetailsService;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
		this.rateLimitingFilter = rateLimitingFilter;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// 비밀번호를 안전하게 해싱하기 위해 BCrypt 알고리즘을 사용하는 PasswordEncoder를 빈으로 등록한다.
		// BCrypt는 강력한 단방향 해싱 함수로, 솔트(salt)를 자동으로 생성하여 무지개 테이블 공격에 강하다.
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		// Spring Security의 인증을 처리하는 핵심 인터페이스인 AuthenticationManager를 빈으로 등록한다.
		// 사용자 인증 요청을 받아 UserDetailsService를 통해 사용자 정보를 로드하고 비밀번호를 검증한다.
		return configuration.getAuthenticationManager();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		// JWT 기반 인증을 위한 커스텀 필터(JwtAuthenticationFilter)를 빈으로 등록한다.
		// 이 필터는 모든 HTTP 요청에 대해 JWT 유효성을 검사하고 인증을 처리한다.
		return new JwtAuthenticationFilter(jwtProvider, userDetailsService);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// 기본 보안 기능 비활성화:
			// - httpBasic: HTTP 기본 인증 비활성화 (JWT 사용)
			// - csrf: CSRF 보호 비활성화 (REST API는 일반적으로 세션 기반이 아니므로 비활성화)
			// - formLogin: 기본 폼 로그인 비활성화 (커스텀 로그인 처리)
			.httpBasic(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)

			// CORS 설정 적용: corsConfigurationSource 빈을 통해 정의된 CORS 정책을 적용한다.
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))

			// 세션 사용 안 함 (JWT 방식):
			// - STATELESS: 서버가 세션을 생성하거나 사용하지 않음을 명시한다. 각 요청은 독립적으로 처리된다.
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// 예외 처리 핸들러:
			// - authenticationEntryPoint: 인증되지 않은 사용자(401 Unauthorized) 접근 시 처리
			// - accessDeniedHandler: 접근 권한이 없는 사용자(403 Forbidden) 접근 시 처리
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(jwtAccessDeniedHandler)
			)

			// 요청별 인가 정책:
			// - requestMatchers(WHITE_LIST).permitAll(): WHITE_LIST에 정의된 URL은 인증 없이 접근 허용
			// - anyRequest().authenticated(): 그 외 모든 요청은 인증된 사용자만 접근 허용
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(WHITE_LIST).permitAll()
				.anyRequest().authenticated()
			)

			// JWT 필터 등록:
			// - UsernamePasswordAuthenticationFilter 이전에 JwtAuthenticationFilter를 추가하여
			//   요청 처리 초기에 JWT 인증을 수행하도록 한다.
			.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		// CORS(Cross-Origin Resource Sharing) 정책을 정의하는 빈을 등록한다.
		// 프론트엔드 애플리케이션이 백엔드 API에 안전하게 접근할 수 있도록 허용되는 오리진, HTTP 메서드, 헤더 등을 설정한다.
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of(allowedOrigins)); // Modified line
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setExposedHeaders(List.of("Authorization")); // JWT 토큰 헤더 노출 허용

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
