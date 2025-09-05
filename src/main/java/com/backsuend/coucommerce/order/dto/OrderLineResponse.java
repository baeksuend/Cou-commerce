package com.backsuend.coucommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * 주문 상품 응답 DTO
 * - OrderResponse 안에서 사용
 * - OrderProduct 엔티티를 기반으로 클라이언트에게 반환
 * - 주문 당시의 상품 정보와 가격 스냅샷을 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLineResponse {

	/**
	 * 상품 ID
	 * - 주문된 상품의 고유 식별자
	 */
	private Long productId;

	/**
	 * 상품 이름
	 * - 주문 당시의 상품명
	 */
	private String name;

	/**
	 * 주문 당시 가격 스냅샷
	 * - 주문 시점의 상품 가격 (변동 가능성 고려)
	 */
	private int priceSnapshot;

	/**
	 * 주문 수량
	 * - 해당 상품의 주문 수량
	 */
	private int quantity;

	/**
	 * 총 금액 (priceSnapshot * quantity)
	 * - 해당 상품의 총 주문 금액
	 */
	private int subtotal;

	/**
	 * 총 금액 계산
	 * @return priceSnapshot * quantity
	 */
	public int getSubtotal() {
		return priceSnapshot * quantity;
	}

	/**
	 * 상품 정보가 유효한지 확인
	 * @return 유효하면 true, 아니면 false
	 */
	public boolean isValid() {
		return productId != null && name != null && !name.trim().isEmpty()
			&& priceSnapshot >= 0 && quantity > 0;
	}
}