package com.backsuend.coucommerce.sellerregistration.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.sellerregistration.dto.CreateSellerRegistrationRequest;
import com.backsuend.coucommerce.sellerregistration.service.SellerRegistrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "판매자 등록 신청 API", description = "구매자가 판매자로 활동하기 위해 등록을 신청하는 API를 제공합니다.")
@RestController
@RequestMapping("/api/v1/seller-registrations")
@RequiredArgsConstructor
public class SellerRegistrationController {

	private final SellerRegistrationService sellerRegistrationService;

	@Operation(summary = "판매자 등록 신청", description = "구매자(BUYER)가 판매자(SELLER)로 활동하기 위해 등록을 신청합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신청 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CREATED",
					value = "{\"success\":true,\"status\":201,\"message\":\"CREATED\",\"data\":null,\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/seller-registrations\",\"errors\":{\"storeName\":\"상점 이름은 필수 입력 값입니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/seller-registrations\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 신청했거나 판매자/관리자임",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CONFLICT",
					value = "{\"success\":false,\"status\":409,\"message\":\"CONFLICT\",\"data\":{\"code\":\"CONFLICT\",\"message\":\"이미 판매자이거나, 판매자 등록 처리 중에 있습니다.\",\"traceId\":null,\"path\":\"/api/v1/seller-registrations\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			))
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> applyForSeller(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@Valid @RequestBody CreateSellerRegistrationRequest request) {
		sellerRegistrationService.apply(userDetails.getId(), request);
		return ApiResponse.<Void>created(null).toResponseEntity();
	}
}
