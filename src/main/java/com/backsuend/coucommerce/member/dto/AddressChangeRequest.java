package com.backsuend.coucommerce.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddressChangeRequest(
	@Schema(description = "우편번호", example = "04538")
	@NotBlank(message = "우편번호는 필수 입력 값입니다.")
	@Size(max = 10, message = "우편번호는 10자를 초과할 수 없습니다.")
	String postalCode,

	@Schema(description = "도로명 주소", example = "서울특별시 중구 세종대로 110")
	@NotBlank(message = "도로명 주소는 필수 입력 값입니다.")
	@Size(max = 100, message = "도로명 주소는 100자를 초과할 수 없습니다.")
	String roadName,

	@Schema(description = "상세 주소", example = "202호")
	@NotBlank(message = "상세 주소는 필수 입력 값입니다.")
	@Size(max = 50, message = "상세 주소는 50자를 초과할 수 없습니다.")
	String detail
) {
}
