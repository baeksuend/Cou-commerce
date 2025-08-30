package com.backsuend.coucommerce.member.dto;

import java.time.LocalDateTime;

import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileResponse(
	@Schema(description = "이메일", example = "user@example.com")
	String email,
	@Schema(description = "이름", example = "홍길동")
	String name,
	@Schema(description = "전화번호", example = "010-1234-5678")
	String phone,
	@Schema(description = "역할", example = "BUYER")
	Role role,
	@Schema(description = "계정 상태", example = "ACTIVE")
	MemberStatus status,
	@Schema(description = "가입 일시", example = "2023-08-29T10:00:00")
	LocalDateTime createdAt,

	@Schema(description = "우편번호", example = "04538")
	String postalCode,
	@Schema(description = "도로명 주소", example = "서울특별시 중구 세종대로 110")
	String roadName,
	@Schema(description = "상세 주소", example = "101호")
	String detail
) {
}
