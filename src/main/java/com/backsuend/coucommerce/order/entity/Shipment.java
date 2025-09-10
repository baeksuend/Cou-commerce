package com.backsuend.coucommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */
@Entity
@Table(name = "shipment",
	indexes = {
		@Index(name = "idx_shipment_order", columnList = "order_id"),
		@Index(name = "idx_shipment_tracking", columnList = "trackingNo")
	})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 어떤 주문의 배송인지 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private Order order;

	/** 운송장 번호 */
	@Column(name = "trackingNo", nullable = false, length = 100)
	private String trackingNo;

	/** 택배사 */
	@Column(name = "carrier", nullable = false, length = 50)
	private String carrier;
}