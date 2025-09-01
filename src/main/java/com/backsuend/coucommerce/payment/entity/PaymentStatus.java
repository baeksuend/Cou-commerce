package com.backsuend.coucommerce.payment.entity;

/**
 * @author rua
 */
public enum PaymentStatus {
	PENDING,   // 결제 요청됨
	APPROVED,  // 결제 성공
	FAILED,    // 결제 실패
	REFUNDED   // 환불 처리됨
}
