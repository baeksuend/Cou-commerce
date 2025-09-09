package com.backsuend.coucommerce.order.controller;

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
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "주문 API", description = "주문 관련 API 입니다.")
@RestController
@PreAuthorize("hasRole('BUYER')") // 클래스 전체 BUYER 전용
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class BuyerOrderController {

	private final OrderService orderService;

	@Operation(summary = "주문 생성 (장바구니에서)", description = "장바구니에 담긴 상품들로 주문을 생성합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "주문 생성 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 상품을 찾을 수 없습니다.")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> createOrderFromCart(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody OrderCreateRequest request) {
		OrderResponse response = orderService.createOrderFromCart(request, userDetails.getId());
		return ApiResponse.created(response).toResponseEntity();
	}

	@Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근이 거부되었습니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없습니다.")
	})
	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
		@Parameter(description = "주문 ID", required = true) @PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		OrderResponse response = orderService.getOrder(orderId, userDetails.getId());
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "내 주문 목록 조회", description = "현재 로그인한 사용자의 모든 주문 내역을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 목록 조회 성공")
	})
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PageableDefault(size = 10) Pageable pageable) {
		Page<OrderResponse> response = orderService.getMyOrders(userDetails.getId(), pageable);
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "주문 취소", description = "특정 주문을 취소합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 취소 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근이 거부되었습니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없습니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "취소할 수 없는 주문 상태입니다.")
	})
	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
		@Parameter(description = "주문 ID", required = true) @PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		OrderResponse response = orderService.cancelOrder(orderId, userDetails.getId());
		return ApiResponse.ok(response).toResponseEntity();
	}
}
