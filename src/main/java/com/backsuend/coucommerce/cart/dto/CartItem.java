package com.backsuend.coucommerce.cart.dto;

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
public class CartItem {
	private Long productId;     // Product.id
	private String name;        // 담을 당시 상품 이름 스냅샷
	private int price;          // 담을 당시 가격 스냅샷
	private int quantity;       // 담은 수량
	private String detail;      // 사용자가 선택한 옵션 (문자열)
}