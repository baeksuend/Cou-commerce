package com.backsuend.coucommerce.payment.logging;

import org.slf4j.MDC;

/**
 * @author rua
 */
public class PaymentLogContext {
	public static void setPaymentContext(Long paymentId, String method, String status) {
		MDC.put("paymentId", String.valueOf(paymentId));
		MDC.put("method", method);
		MDC.put("status", status);
	}

	public static void clear() {
		MDC.remove("paymentId");
		MDC.remove("method");
		MDC.remove("status");
	}
}
