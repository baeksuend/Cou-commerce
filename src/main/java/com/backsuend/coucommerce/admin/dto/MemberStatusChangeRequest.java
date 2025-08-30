package com.backsuend.coucommerce.admin.dto;

import jakarta.validation.constraints.NotNull;

import com.backsuend.coucommerce.auth.entity.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberStatusChangeRequest(
	@Schema(description = "변경할 회원의 새로운 상태", example = "LOCKED")
	@NotNull(message = "새로운 상태는 필수 입력 값입니다.")
	MemberStatus newStatus
) {
}
