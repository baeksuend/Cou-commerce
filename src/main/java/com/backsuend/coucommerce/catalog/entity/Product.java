package com.backsuend.coucommerce.catalog.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rua
 */

@Getter
@Setter
@Entity
@Table(name = "product",
	indexes = {
		@Index(name = "idx_product_member", columnList = "member_id"),
		@Index(name = "idx_product_category", columnList = "category"),
		@Index(name = "idx_product_visible", columnList = "is_status")
	})
public class Product extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// SELLER 소유자
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@NotBlank
	@Size(max = 50)
	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@NotBlank
	@Lob
	@Column(name = "detail", nullable = false)
	private String detail;

	@Min(0)
	@Column(name = "stock", nullable = false)
	private int stock;

	@Min(0)
	@Column(name = "price", nullable = false)
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 20)
	private Category category;

	/** 공개 여부 (is_status) */
	@Column(name = "is_status", nullable = false)
	private boolean visible = true;

	public Product(Long id, Member member, String name, String detail, int stock, int price, Category category,
		boolean visible, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
		this.id = id;
		this.member = member;
		this.name = name;
		this.detail = detail;
		this.stock = stock;
		this.price = price;
		this.category = category;
		this.visible = visible;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.deletedAt = deletedAt;
	}

	public Product() {

	}
}