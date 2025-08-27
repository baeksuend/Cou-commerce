package com.backsuend.coucommerce.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// httpBasic, csrf, formLogin, sessionManagement 비활성화
			.httpBasic(httpBasic -> httpBasic.disable())
			.csrf(csrf -> csrf.disable())
			.formLogin(formLogin -> formLogin.disable())
			.sessionManagement(sessionManagement ->
				sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			// 모든 요청 허용
			.authorizeHttpRequests(authorize -> authorize
				.anyRequest().permitAll()
			);
		return http.build();
	}
}
