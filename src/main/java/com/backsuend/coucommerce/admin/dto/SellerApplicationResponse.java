package com.backsuend.coucommerce.admin.dto;

import java.time.LocalDateTime;

import com.backsuend.coucommerce.seller.entity.SellerStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record SellerApplicationResponse(
	@Schema(description = "신청 ID")
	Long applicationId,
	@Schema(description = "신청자 이메일")
	String userEmail,
	@Schema(description = "신청자 이름")
	String userName,
	@Schema(description = "상호명")
	String storeName,
	@Schema(description = "사업자 등록 번호")
	String businessRegistrationNumber,
	@Schema(description = "신청 상태")
	SellerStatus status,
	@Schema(description = "신청 일시")
	LocalDateTime createdAt
) {
}
