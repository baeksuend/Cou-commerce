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
	description = "토큰 원본 사용 (Bearer 없이)"
)
@SecurityScheme(
	name = "Refresh-Token",
	type = SecuritySchemeType.APIKEY,
	in = SecuritySchemeIn.HEADER,
	paramName = "Refresh-Token",
	description = "접미사로 \"Bearer \" 추가해야 함 (공백 주의)"
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
