package com.backsuend.coucommerce.order.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

/**
 * @author rua
 */
@Entity
@Table(name = "orders", // 예약어 회피를 위해 orders 권장
	indexes = {
		@Index(name = "idx_orders_member", columnList = "member_id"),
		@Index(name = "idx_orders_status", columnList = "status")
	})
public class Order extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY) // BUYER
	@JoinColumn(name = "member_id", nullable = false)
	private Member buyer;

	@Column(name = "consumer_name", nullable = false, length = 20)
	private String consumerName;

	@Column(name = "consumer_phone", nullable = false, length = 20)
	private String consumerPhone;

	@Column(name = "receiver_name", nullable = false, length = 20)
	private String receiverName;

	@Column(name = "receiver_road_name", nullable = false, length = 100)
	private String receiverRoadName;

	@Column(name = "receiver_phone", nullable = false, length = 20)
	private String receiverPhone;

	@Column(name = "receiver_postal_code", nullable = false, length = 10)
	private String receiverPostalCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private OrderStatus status = OrderStatus.READY;

	/** 스키마 상 존재하는 payment_id(FK). 양방향 원하면 Payment 쪽에도 order_id 매핑. */
	@Column(name = "payment_id")
	private Long paymentId;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderProduct> items = new ArrayList<>();

	// 편의 메서드
	public void addItem(OrderProduct item) {
		items.add(item);
		item.setOrder(this);
	}
}
