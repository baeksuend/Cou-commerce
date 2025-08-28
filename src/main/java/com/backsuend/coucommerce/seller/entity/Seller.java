package com.backsuend.coucommerce.seller.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

/**
 * @author rua
 */
@Entity
@Table(name = "seller",
	uniqueConstraints = @UniqueConstraint(name = "uk_seller_member", columnNames = "member_id"),
	indexes = {
		@Index(name = "idx_seller_member", columnList = "member_id"),
		@Index(name = "idx_seller_status", columnList = "status")
	})
public class Seller extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** SELLER 신청한 멤버 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SellerStatus status = SellerStatus.APPLIED;

	/** 승인한 관리자(Member) – null 허용 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_member_id")
	private Member approvedBy;

	@Lob
	@Column(name = "reason")
	private String reason;
}
