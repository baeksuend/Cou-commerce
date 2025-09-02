package com.backsuend.coucommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * 주문 상품 요청 DTO
 * - OrderCreateRequest 안에서 사용
 * - 주문 시 장바구니의 각 상품을 OrderProduct로 변환하기 위함
 * - 상품 ID와 주문 수량 정보를 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

	/**
	 * 상품 ID
	 * - 주문할 상품의 고유 식별자
	 * - null이 아니어야 함
	 */
	@NotNull(message = "상품 ID는 필수입니다.")
	private Long productId;

	/**
	 * 주문 수량
	 * - 해당 상품의 주문 수량
	 * - 최소 1개 이상이어야 함
	 */
	@Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
	private int quantity;

	/**
	 * 상품 정보가 유효한지 확인
	 * @return 유효하면 true, 아니면 false
	 */
	public boolean isValid() {
		return productId != null && quantity > 0;
	}
}