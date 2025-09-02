package com.backsuend.coucommerce.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "장바구니 API", description = "장바구니 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@Operation(summary = "장바구니 조회", description = "현재 로그인한 사용자의 장바구니 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "장바구니 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.")
	})
	@GetMapping("/items")
	public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserDetailsImpl user) {
		Long memberId = user.getId();
		CartResponse cartResponse = cartService.getCart(memberId);
		return ApiResponse.ok(cartResponse).toResponseEntity();
	}

	@Operation(summary = "장바구니 상품 추가", description = "장바구니에 상품을 추가합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "상품 추가 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.")
	})
	@PostMapping("/items")
	public ResponseEntity<ApiResponse<CartResponse>> addItem(@AuthenticationPrincipal UserDetailsImpl user,
		@RequestBody CartItem item) {
		Long memberId = user.getId();
		CartResponse cartResponse = cartService.addItem(memberId, item);
		return ApiResponse.created(cartResponse).toResponseEntity();
	}

	@Operation(summary = "장바구니 상품 수정", description = "장바구니에 담긴 상품의 수량 등 정보를 수정합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 수정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없습니다.")
	})
	@PutMapping("/items")
	public ResponseEntity<ApiResponse<CartResponse>> updateItem(@AuthenticationPrincipal UserDetailsImpl user,
		@RequestBody CartItem item) {
		Long memberId = user.getId();
		CartResponse cartResponse = cartService.updateItem(memberId, item);
		return ApiResponse.ok(cartResponse).toResponseEntity();
	}

	@Operation(summary = "장바구니 상품 삭제", description = "장바구니에서 특정 상품을 제거합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 삭제 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없습니다.")
	})
	@DeleteMapping("/items/{productId}")
	public ResponseEntity<ApiResponse<CartResponse>> removeItem(@AuthenticationPrincipal UserDetailsImpl user,
		@Parameter(description = "상품 ID", required = true) @PathVariable Long productId) {
		Long memberId = user.getId();
		CartResponse cartResponse = cartService.removeItem(memberId, productId);
		return ApiResponse.ok(cartResponse).toResponseEntity();
	}
}
