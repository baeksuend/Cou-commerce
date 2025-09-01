package com.backsuend.coucommerce.payment.dto;

import java.time.LocalDateTime;

import com.backsuend.coucommerce.payment.entity.CardBrand;
import com.backsuend.coucommerce.payment.entity.Payment;
import com.backsuend.coucommerce.payment.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * 결제 응답 DTO
 * - 결제 요청이 처리된 후 클라이언트에게 반환되는 데이터
 * - 결제 정보 조회 시에도 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

	/**
	 * 결제 ID (고유 식별자)
	 */
	private Long paymentId;

	/**
	 * 주문 ID (결제가 연결된 주문)
	 */
	private Long orderId;

	/**
	 * 결제 금액
	 */
	private int amount;

	/**
	 * 카드 브랜드 (예: KB, SH, KAKAO)
	 */
	private CardBrand cardBrand;

	/**
	 * 결제 상태
	 * - PENDING: 결제 대기
	 * - APPROVED: 결제 승인
	 * - FAILED: 결제 실패
	 * - REFUNDED: 환불 완료
	 */
	private PaymentStatus status;

	/**
	 * 외부 트랜잭션 ID
	 * - Mock 결제의 경우 "MOCK-{timestamp}" 형식으로 생성
	 * - 실제 PG사 연동 시 PG사에서 제공하는 트랜잭션 ID
	 */
	private String transactionId;

	/**
	 * 주문 상태
	 * - Order 엔티티의 현재 상태 (PLACED, PAID, CANCELED 등)
	 */
	private String orderStatus;

	/**
	 * 결제 생성 시간
	 */
	private LocalDateTime createdAt;

	/**
	 * 결제 완료 시간 (승인/실패 시점)
	 */
	private LocalDateTime updatedAt;

	/**
	 * Entity → DTO 변환 편의 메서드
	 */
	public static PaymentResponse from(Payment payment) {
		return PaymentResponse.builder()
			.paymentId(payment.getId())
			.orderId(payment.getOrder().getId())
			.amount(payment.getAmount())
			.cardBrand(payment.getCardBrand())
			.status(payment.getStatus())
			.transactionId(payment.getTransactionId())
			.orderStatus(payment.getOrder().getStatus().name())
			.createdAt(payment.getCreatedAt())
			.updatedAt(payment.getUpdatedAt())
			.build();
	}

	/**
	 * 결제가 승인되었는지 확인
	 * @return APPROVED 상태면 true, 아니면 false
	 */
	public boolean isApproved() {
		return PaymentStatus.APPROVED.equals(status);
	}

	/**
	 * 결제가 실패했는지 확인
	 * @return FAILED 상태면 true, 아니면 false
	 */
	public boolean isFailed() {
		return PaymentStatus.FAILED.equals(status);
	}

	/**
	 * 결제가 대기 중인지 확인
	 * @return PENDING 상태면 true, 아니면 false
	 */
	public boolean isPending() {
		return PaymentStatus.PENDING.equals(status);
	}

	/**
	 * 결제 정보가 유효한지 확인
	 * @return 유효하면 true, 아니면 false
	 */
	public boolean isValid() {
		return paymentId != null && orderId != null && amount > 0
			&& cardBrand != null && status != null && transactionId != null;
	}
}