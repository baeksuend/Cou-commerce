package com.backsuend.coucommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
	@Schema(description = "사용자 이메일", example = "user@example.com")
	@Email(message = "유효한 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	String email,

	@Schema(description = "비밀번호", example = "password123!")
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	String password
) {
}
