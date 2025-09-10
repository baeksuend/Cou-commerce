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

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */
@RestController
@RequestMapping("/api/v1/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerOrderController {
	private final OrderService orderService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
		@AuthenticationPrincipal UserDetailsImpl seller) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.getSellerOrders(seller.getId())));
	}

    @PatchMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
        @PathVariable Long orderId,
        @AuthenticationPrincipal UserDetailsImpl seller,
        @RequestBody ShipOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.shipOrder(orderId, seller.getId(), request)));
    }

    @PatchMapping("/{orderId}/approve-refund")
    public ResponseEntity<ApiResponse<OrderResponse>> approveRefund(
        @PathVariable Long orderId,
        @AuthenticationPrincipal UserDetailsImpl seller) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.approveRefund(orderId, seller.getId())));
    }

    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
        @PathVariable Long orderId,
        @AuthenticationPrincipal UserDetailsImpl seller) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.completeOrder(orderId, seller.getId())));
    }
}
