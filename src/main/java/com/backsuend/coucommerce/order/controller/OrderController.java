package com.backsuend.coucommerce.order.controller;

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
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.service.OrderService;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	/**
	 * 주문 생성 (장바구니에서)
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> createOrderFromCart(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody OrderCreateRequest request) {
		OrderResponse response = orderService.createOrderFromCart(request, userDetails.getId());
		return ApiResponse.created(response).toResponseEntity();
	}

	/**
	 * 주문 상세 조회
	 */
	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
		@PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		OrderResponse response = orderService.getOrder(orderId, userDetails.getId());
		return ApiResponse.ok(response).toResponseEntity();
	}

	/**
	 * 내 주문 목록 조회
	 */
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PageableDefault(size = 10) Pageable pageable) {
		Page<OrderResponse> response = orderService.getMyOrders(userDetails.getId(), pageable);
		return ApiResponse.ok(response).toResponseEntity();
	}

	/**
	 * 주문 취소
	 */
	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
		@PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		OrderResponse response = orderService.cancelOrder(orderId, userDetails.getId());
		return ApiResponse.ok(response).toResponseEntity();
	}
}