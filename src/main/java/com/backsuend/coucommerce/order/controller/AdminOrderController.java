package com.backsuend.coucommerce.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.service.OrderService;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

	private final OrderService orderService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
		@RequestParam(required = false) Long buyerId,
		@RequestParam(required = false) Long sellerId,
		@RequestParam(required = false) OrderStatus status) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(buyerId, sellerId, status)));
	}

	@PatchMapping("/{id}/cancel")
	public ResponseEntity<ApiResponse<OrderResponse>> forceCancel(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.forceCancel(id)));
	}

	@PatchMapping("/{id}/refund")
	public ResponseEntity<ApiResponse<OrderResponse>> forceRefund(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(orderService.forceRefund(id)));
	}
}
