package com.backsuend.coucommerce.order.logging;

import org.slf4j.MDC;

/**
 * @author rua
 */
public class OrderLogContext {
	public static void setOrderContext(Long orderId, String status, int totalAmount) {
		MDC.put("orderId", String.valueOf(orderId));
		MDC.put("status", status);
		MDC.put("totalAmount", String.valueOf(totalAmount));
	}

	public static void clear() {
		MDC.remove("orderId");
		MDC.remove("status");
		MDC.remove("totalAmount");
	}
}