package com.backsuend.coucommerce.cart.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
	private String cartId;      // ex) "cart:{memberId}"
	private List<CartItem> items;

	/**
	 * 장바구니가 비어있는지 확인
	 */
	public boolean isEmpty() {
		return items == null || items.isEmpty();
	}

	/**
	 * 장바구니 아이템 개수 반환
	 */
	public int getItemCount() {
		return items != null ? items.size() : 0;
	}
}
