package com.backsuend.coucommerce.order.entity;

/**
 * @author rua
 */
public enum OrderStatus {
	PLACED,     // 주문 생성됨
	PAID,       // 결제 완료
	SHIPPED,    // 배송중
	COMPLETED,  // 배송완료
	CANCELED    // 취소됨
}

