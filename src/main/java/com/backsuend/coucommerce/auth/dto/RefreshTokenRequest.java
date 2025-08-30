package com.backsuend.coucommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenRequest(
	@Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIJleH...")
	@NotBlank(message = "리프레시 토큰은 필수 입력 값입니다.")
	String refreshToken
) {
}
