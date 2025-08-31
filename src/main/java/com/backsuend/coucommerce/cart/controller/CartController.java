package com.backsuend.coucommerce.cart.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	// 장바구니 조회
	@GetMapping("/{memberId}")
	public ResponseEntity<CartResponse> getCart(@PathVariable Long memberId) throws JsonProcessingException {
		return ResponseEntity.ok(cartService.getCart(memberId));
	}

	// 장바구니 상품 추가
	@PostMapping("/{memberId}/items")
	public ResponseEntity<?> addItem(@PathVariable Long memberId,
		@RequestBody CartItem item) throws JsonProcessingException {
		cartService.addItem(memberId, item);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true));
	}

	// 장바구니 상품 수정
	@PutMapping("/{memberId}/items/{productId}")
	public ResponseEntity<?> updateItem(@PathVariable Long memberId,
		@PathVariable Long productId,
		@RequestBody CartItem item) throws JsonProcessingException {
		// productId를 item에 설정하여 일관성 유지
		item.setProductId(productId);
		cartService.updateItem(memberId, item);
		return ResponseEntity.ok(Map.of("success", true));
	}

	// 장바구니 상품 삭제
	@DeleteMapping("/{memberId}/items/{productId}")
	public ResponseEntity<?> removeItem(@PathVariable Long memberId,
		@PathVariable Long productId) {
		cartService.removeItem(memberId, productId);
		return ResponseEntity.ok(Map.of("success", true));
	}
}
