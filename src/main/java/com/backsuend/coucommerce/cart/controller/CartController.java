package com.backsuend.coucommerce.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.backsuend.coucommerce.cart.service.IdService;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

/**
 * ToDo
 * 1. IdService 위치 조정 필요. 현재 기능은 저장된 유저 정보인 멤버의 email을 통해 장바구니를 만들때 필요한 id를 추출.
 * */

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;
	private final IdService idService;

	// 장바구니 조회
	@GetMapping("/")
	public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserDetails user) {
		String email = user.getUsername();
		Long memberid = idService.getMemberIdByEmail(email);
		CartResponse cartResponse = cartService.getCart(memberid);
		return ApiResponse.ok(cartResponse).toResponseEntity();
	}

	// 장바구니 상품 추가
	@PostMapping("/items")
	public ResponseEntity<ApiResponse<CartResponse>> addItem(@AuthenticationPrincipal UserDetails user,
		@RequestBody CartItem item) {
		String email = user.getUsername();
		Long memberid = idService.getMemberIdByEmail(email);
		CartResponse cartResponse = cartService.addItem(memberid, item);
		return ApiResponse.created(cartResponse).toResponseEntity();
	}

	// 장바구니 상품 수정
	@PutMapping("/items/{productId}")
	public ResponseEntity<ApiResponse<CartResponse>> updateItem(@AuthenticationPrincipal UserDetails user,
		@PathVariable Long productId,
		@RequestBody CartItem item) {
		String email = user.getUsername();
		Long memberid = idService.getMemberIdByEmail(email);
		// productId를 item에 설정하여 일관성 유지
		item.setProductId(productId);
		CartResponse cartResponse = cartService.updateItem(memberid, item);
		return ApiResponse.ok(cartResponse).toResponseEntity();
	}

	// 장바구니 상품 삭제
	@DeleteMapping("/items/{productId}")
	public ResponseEntity<ApiResponse<CartResponse>> removeItem(@AuthenticationPrincipal UserDetails user,
		@PathVariable Long productId) {
		String email = user.getUsername();
		Long memberid = idService.getMemberIdByEmail(email);
		CartResponse cartResponse = cartService.removeItem(memberid, productId);
		return ApiResponse.ok(cartResponse).toResponseEntity();
	}
}
