package com.backsuend.coucommerce.payment.entity;

/**
 * @author rua
 */

/**
 * 결제 상태
 * - PENDING: 결제 요청만 들어온 상태
 * - APPROVED: 결제 승인 성공
 * - FAILED: 결제 실패
 * - REFUNDED: 환불 완료
 */
public enum PaymentStatus {
	PENDING,
	APPROVED,
	FAILED,
	REFUNDED
}