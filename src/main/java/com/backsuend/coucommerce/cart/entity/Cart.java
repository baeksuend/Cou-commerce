package com.backsuend.coucommerce.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

/**
 * @author rua
 */

@Entity
@Table(name = "cart",
	uniqueConstraints = @UniqueConstraint(name = "uk_cart_member_product", columnNames = {"member_id", "product_id"}),
	indexes = {
		@Index(name = "idx_cart_member", columnList = "member_id"),
		@Index(name = "idx_cart_product", columnList = "product_id")
	})
public class Cart extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY) // 구매자
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Min(1)
	@Column(name = "quantity", nullable = false)
	private int quantity;

}
