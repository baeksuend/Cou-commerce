package com.backsuend.coucommerce.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record SellerApplicationRequest(
	@Schema(description = "상호명", example = "쿠커머스")
	@NotBlank(message = "상호명은 필수 입력 값입니다.")
	@Size(max = 100, message = "상호명은 100자를 초과할 수 없습니다.")
	String storeName,

	@Schema(description = "사업자 등록 번호", example = "123-45-67890")
	@NotBlank(message = "사업자 등록 번호는 필수 입력 값입니다.")
	@Size(max = 50, message = "사업자 등록 번호는 50자를 초과할 수 없습니다.")
	String businessRegistrationNumber
) {
}
