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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 주문과 1:1 (스키마에 맞춰 order_id FK 보유) */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private Order order;

	/** 카드 브랜드 */
	@Enumerated(EnumType.STRING)
	@Column(name = "card_brand", nullable = false, length = 50)
	private CardBrand cardBrand;

	/** 결제 금액 */
	@Min(0)
	@Column(name = "amount", nullable = false)
	private int amount;

	/** 결제 상태 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status = PaymentStatus.PENDING;

	/** (선택) 외부 PG 트랜잭션 번호 */
	@Column(name = "transaction_id", length = 100, unique = true)
	private String transactionId;
}
