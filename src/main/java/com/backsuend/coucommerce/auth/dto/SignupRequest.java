package com.backsuend.coucommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupRequest(
	@Schema(description = "이메일", example = "user@example.com")
	@Email(message = "유효한 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	String email,

	@Schema(description = "비밀번호 (최소 8자 이상)", example = "password123!")
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	@Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
	String password,

	@Schema(description = "사용자 이름", example = "홍길동")
	@NotBlank(message = "이름은 필수 입력 값입니다.")
	@Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
	String name,

	@Schema(description = "전화번호", example = "010-1234-5678")
	@NotBlank(message = "전화번호는 필수 입력 값입니다.")
	@Size(max = 50, message = "전화번호는 50자를 초과할 수 없습니다.")
	String phone,

	@Schema(description = "우편번호", example = "04538")
	@NotBlank(message = "우편번호는 필수 입력 값입니다.")
	@Size(max = 10, message = "우편번호는 10자를 초과할 수 없습니다.")
	String postalCode,

	@Schema(description = "도로명 주소", example = "서울특별시 중구 세종대로 110")
	@NotBlank(message = "도로명 주소는 필수 입력 값입니다.")
	@Size(max = 100, message = "도로명 주소는 100자를 초과할 수 없습니다.")
	String roadName,

	@Schema(description = "상세 주소", example = "101호")
	@NotBlank(message = "상세 주소는 필수 입력 값입니다.")
	@Size(max = 50, message = "상세 주소는 50자를 초과할 수 없습니다.")
	String detail
) {
}
