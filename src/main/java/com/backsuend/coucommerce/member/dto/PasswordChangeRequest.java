package com.backsuend.coucommerce.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record PasswordChangeRequest(
	@Schema(description = "현재 비밀번호", example = "password123!")
	@NotBlank(message = "현재 비밀번호는 필수 입력 값입니다.")
	String oldPassword,

	@Schema(description = "새 비밀번호 (최소 8자 이상)", example = "newPassword123!")
	@NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
	@Size(min = 8, message = "새 비밀번호는 최소 8자 이상이어야 합니다.")
	String newPassword
) {
}
