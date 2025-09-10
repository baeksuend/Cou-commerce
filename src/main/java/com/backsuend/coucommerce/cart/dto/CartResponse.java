package com.backsuend.coucommerce.cart.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * Cart 응답 스키마 (PRD)
 * items: CartItemResponse[]
 * totalPrice: 합계 (quantity * priceAtAdd)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
	private List<CartItem> items;
	private Integer totalPrice;

	/**
	 * 장바구니가 비어있는지 확인
	 */
	public boolean isEmpty() {
		return items == null || items.isEmpty();
	}
}
