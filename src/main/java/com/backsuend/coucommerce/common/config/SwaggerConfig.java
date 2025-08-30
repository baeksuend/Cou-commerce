package com.backsuend.coucommerce.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI 3.0 (Swagger) 문서 생성을 위한 설정 클래스.
 * API 서버 정보, 보안 스키마(JWT Access Token, Refresh Token) 등을 정의하여
 * API 문서의 정확성과 사용 편의성을 높인다.
 */
@OpenAPIDefinition(
	servers = {
		@Server(url = "http://localhost:8080", description = "로컬 서버")
	})
@Configuration
@SecurityScheme(
	name = "Authorization",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT",
	description = "JWT Access Token"
)
@SecurityScheme(
	name = "Refresh-Token",
	type = SecuritySchemeType.APIKEY,
	in = SecuritySchemeIn.HEADER,
	paramName = "Refresh-Token",
	description = "액세스 토큰 재발급을 위한 Refresh Token"
)
public class SwaggerConfig {
	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.components(new Components())
			.info(apiInfo());
	}

	private Info apiInfo() {
		return new Info()
			.title("Cou-commerce Swagger")
			.description("모든 REST API")
			.version("1.0.0");
	}
}
