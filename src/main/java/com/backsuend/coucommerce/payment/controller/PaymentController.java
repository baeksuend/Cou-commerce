package com.backsuend.coucommerce.payment.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
import com.backsuend.coucommerce.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

/**
 * Payment Controller
 * - Buyer가 결제를 요청하고 결제 정보를 확인하는 API 엔드포인트
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	/**
	 * 결제 진행 API
	 * @param orderId 주문 ID
	 * @param request 결제 요청 DTO
	 * @return PaymentResponse
	 */
	@PostMapping("/{orderId}")
	public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PathVariable Long orderId,
		@RequestBody PaymentRequest request
	) {
		PaymentResponse response = paymentService.processPayment(userDetails.getId(), orderId, request);
		return ApiResponse.ok(response).toResponseEntity();
	}

	/**
	 * 특정 주문의 결제 정보 조회 API
	 * @param orderId 주문 ID
	 * @return PaymentResponse
	 */
	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PathVariable Long orderId
	) {
		PaymentResponse response = paymentService.getPayment(userDetails.getId(), orderId);
		return ApiResponse.ok(response).toResponseEntity();
	}

	/**
	 * Buyer의 모든 결제 내역 조회 API (페이징)
	 * @param pageable 페이징 정보
	 * @return Page<PaymentResponse>
	 */
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PageableDefault(size = 10) Pageable pageable
	) {
		Page<PaymentResponse> responses = paymentService.getMyPayments(userDetails.getId(), pageable);
		return ApiResponse.ok(responses).toResponseEntity();
	}
}