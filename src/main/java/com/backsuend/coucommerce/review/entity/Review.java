package com.backsuend.coucommerce.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

/**
 * @author rua
 */

@Entity
@Table(name = "review",
	indexes = {
		@Index(name = "idx_review_member", columnList = "member_id"),
		@Index(name = "idx_review_product", columnList = "product_id")
	})
public class Review extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY) // 작성자
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Lob
	@Column(name = "content")
	private String content;
}
