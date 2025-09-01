package com.backsuend.coucommerce.admin.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record SellerRejectionRequest(
	@Schema(description = "거절 사유")
	@NotBlank(message = "거절 사유는 필수 입력 값입니다.")
	String reason
) {
}
