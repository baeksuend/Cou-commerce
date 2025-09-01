package com.backsuend.coucommerce.cart.dto;

import java.util.List;

import lombok.AllArgsConstructor;
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
public class CartResponse {
	private String cartId;      // ex) "cart:{memberId}"
	private List<CartItem> items;
}
