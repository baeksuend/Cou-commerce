package com.backsuend.coucommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public record AuthResponse(
	@Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1...")
	String accessToken,
	@Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...")
	String refreshToken
) {
}
