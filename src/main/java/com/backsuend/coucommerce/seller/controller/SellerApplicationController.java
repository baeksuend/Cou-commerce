package com.backsuend.coucommerce.seller.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.seller.dto.SellerApplicationRequest;
import com.backsuend.coucommerce.seller.service.SellerApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "판매자 API", description = "판매자 전환 신청 등")
@RestController
@RequestMapping("/api/v1/seller-app")
@RequiredArgsConstructor
public class SellerApplicationController {

	private final SellerApplicationService sellerApplicationService;

	@Operation(summary = "판매자 전환 신청", description = "구매자(BUYER)가 판매자(SELLER)로 전환하기 위해 신청합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신청 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 신청했거나 판매자/관리자임")
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/apply")
	public ResponseEntity<ApiResponse<Void>> applyForSeller(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@Valid @RequestBody SellerApplicationRequest request) {
		sellerApplicationService.apply(userDetails.getId(), request);
		return ApiResponse.<Void>created(null).toResponseEntity();
	}
}
