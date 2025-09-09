package com.backsuend.coucommerce.payment.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.payment.dto.PaymentRequest;
import com.backsuend.coucommerce.payment.dto.PaymentResponse;
import com.backsuend.coucommerce.payment.dto.RefundRequest;
import com.backsuend.coucommerce.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "결제 API", description = "결제 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('BUYER')") // 클래스 전체 BUYER 전용
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@Operation(summary = "결제 진행", description = "특정 주문에 대한 결제를 진행합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "결제 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근이 거부되었습니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없습니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "결제할 수 없는 주문 상태이거나 이미 결제가 진행된 주문입니다.")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> processPaymentNew(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody @Valid PaymentRequest request
	) {
		PaymentResponse response = paymentService.processPayment(
			userDetails.getId(), request.getOrderId(), request
		);
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "결제 정보 조회", description = "특정 주문의 결제 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "결제 정보 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근이 거부되었습니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 또는 결제 정보를 찾을 수 없습니다.")
	})
	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@Parameter(description = "주문 ID", required = true) @PathVariable Long orderId
	) {
		PaymentResponse response = paymentService.getPayment(userDetails.getId(), orderId);
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "내 결제 내역 조회", description = "현재 로그인한 사용자의 모든 결제 내역을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "결제 내역 조회 성공")
	})
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PageableDefault(size = 10) Pageable pageable
	) {
		Page<PaymentResponse> responses = paymentService.getMyPayments(userDetails.getId(), pageable);
		return ApiResponse.ok(responses).toResponseEntity();
	}

	@PostMapping("/{paymentId}/refund-request")
	@Operation(summary = "환불 요청", description = "Buyer가 결제 건에 대해 환불을 요청합니다.")
	public ResponseEntity<ApiResponse<PaymentResponse>> requestRefund(
		@AuthenticationPrincipal UserDetailsImpl buyer,
		@PathVariable Long paymentId,
		@RequestBody RefundRequest request
	) {
		PaymentResponse response = paymentService.requestRefund(buyer.getId(), paymentId, request.getReason());
		return ApiResponse.ok(response).toResponseEntity();
	}
}
