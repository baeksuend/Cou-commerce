package com.backsuend.coucommerce.admin.dto;

import java.time.LocalDateTime;

import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record SellerRegistrationResponse(
	@Schema(description = "등록 신청 ID", example = "1")
	Long registrationId,
	@Schema(description = "신청자 이메일", example = "seller@example.com")
	String userEmail,
	@Schema(description = "신청자 이름", example = "김상인")
	String userName,
	@Schema(description = "상호명", example = "판매상점")
	String storeName,
	@Schema(description = "사업자 등록 번호", example = "123-45-67890")
	String businessRegistrationNumber,
	@Schema(description = "신청 상태", example = "APPLIED")
	SellerRegistrationStatus status,
	@Schema(description = "신청 일시", example = "2025-09-01T14:00:00")
	LocalDateTime createdAt
) {
}
