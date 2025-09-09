package com.backsuend.coucommerce.cart.logging;

import org.slf4j.MDC;

/**
 * @author rua
 */
public class CartLogContext {
	public static void setCartContext(Long cartId, Long productId, int quantity) {
		MDC.put("cartId", String.valueOf(cartId));
		MDC.put("productId", String.valueOf(productId));
		MDC.put("quantity", String.valueOf(quantity));
	}

	public static void clear() {
		MDC.remove("cartId");
		MDC.remove("productId");
		MDC.remove("quantity");
	}
}