package com.backsuend.coucommerce.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.dto.ShipOrderRequest;
import com.backsuend.coucommerce.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */
@Tag(name = "주문 API(판매자)", description = "판매자 주문 처리 API 입니다.")
@RestController
@RequestMapping("/api/v1/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerOrderController {
	private final OrderService orderService;

	@Operation(summary = "판매자 주문 목록 조회", description = "현재 로그인한 판매자의 상품이 포함된 주문들을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 또는 실패")
	})
	@GetMapping
	public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
		@AuthenticationPrincipal UserDetailsImpl seller) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.getSellerOrders(seller.getId())));
	}

	@Operation(summary = "배송 시작 처리", description = "운송장 정보를 입력하고 주문을 배송중(SHIPPED) 상태로 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 또는 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "상태 불일치")
	})
	@PatchMapping("/{orderId}/ship")
	public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
		@Parameter(description = "주문 ID", required = true) @PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl seller,
		@RequestBody ShipOrderRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.shipOrder(orderId, seller.getId(), request)));
	}

	@Operation(summary = "환불 승인", description = "환불 요청된 주문을 환불 처리(REFUNDED)합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 또는 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "승인 불가 상태")
	})
	@PatchMapping("/{orderId}/approve-refund")
	public ResponseEntity<ApiResponse<OrderResponse>> approveRefund(
		@Parameter(description = "주문 ID", required = true) @PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl seller) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.approveRefund(orderId, seller.getId())));
	}

	@Operation(summary = "배송 완료 처리", description = "SHIPPED 상태의 주문을 COMPLETED 상태로 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 또는 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "상태 불일치")
	})
	@PatchMapping("/{orderId}/complete")
	public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
		@Parameter(description = "주문 ID", required = true) @PathVariable Long orderId,
		@AuthenticationPrincipal UserDetailsImpl seller) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.completeOrder(orderId, seller.getId())));
	}
}
