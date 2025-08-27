package com.backsuend.coucommerce.payment.entity;

import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import com.backsuend.coucommerce.order.entity.Order;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

/**
 * @author rua
 */
@Entity
@Table(name = "payment",
        indexes = @Index(name = "idx_payment_order", columnList = "order_id"))
public class Payment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문과 1:1 (스키마에 맞춰 order_id FK 보유)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_id", nullable = false, length = 50)
    private CardBrand cardId;

    @Min(0)
    @Column(name = "total_price", nullable = false)
    private int totalPrice;
}
