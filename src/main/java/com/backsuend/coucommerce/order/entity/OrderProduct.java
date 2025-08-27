package com.backsuend.coucommerce.order.entity;

import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

/**
 * @author rua
 */

@Entity
@Table(name = "order_product",
        indexes = {
                @Index(name = "idx_order_product_order", columnList = "order_id"),
                @Index(name = "idx_order_product_product", columnList = "product_id")
        })
public class OrderProduct extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(1)
    @Column(name = "quantity", nullable = false)
    private int quantity;

    public void setOrder(Order order) { this.order = order; }
}
