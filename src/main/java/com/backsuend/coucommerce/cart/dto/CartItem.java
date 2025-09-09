package com.backsuend.coucommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
	@NotNull
	@Positive
	private Long productId;     // Product.id

	@Size(max = 255)
	private String productName;        // 담을 당시 상품 이름 스냅샷

	@Min(0)
	private int priceAtAdd;          // 담을 당시 가격 스냅샷

	@NotNull
	private int quantity;       // 담은 수량

	@Size(max = 255)
	private String detail;      // 사용자가 선택한 옵션 (문자열)
}
