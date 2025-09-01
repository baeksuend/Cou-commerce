package com.backsuend.coucommerce.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import com.backsuend.coucommerce.order.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * Payment (결제 엔티티)
 * - Order와 1:1 매핑
 * - 결제 요청/승인/실패/환불 상태를 관리
 * - 실제 PG 연동이 아닌 Mock 기반으로 결제 시뮬레이션 가능
 */
@Entity
@Table(name = "payment",
	indexes = {
		@Index(name = "idx_payment_order", columnList = "order_id"),
		@Index(name = "idx_payment_status", columnList = "status")
	})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseTimeEntity {

	/** 고유 결제 ID */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 주문 (Order)와 1:1 관계 (결제 대상 주문) */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private Order order;

	/** 카드 브랜드 (예: KB, SH, KAKAO) */
	@Enumerated(EnumType.STRING)
	@Column(name = "card_brand", nullable = false, length = 50)
	private CardBrand cardBrand;

	/** 결제 금액 (주문 총액과 일치해야 함) */
	@Min(0)
	@Column(name = "amount", nullable = false)
	private int amount;

	/** 결제 상태 (PENDING → APPROVED/FAILED/REFUNDED) */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private PaymentStatus status = PaymentStatus.PENDING;

	/** 외부 PG사 트랜잭션 번호 (Mock일 경우 가짜 ID 발급) */
	@Column(name = "transaction_id", length = 100, unique = true)
	private String transactionId;
}
