package com.backsuend.coucommerce.sellerregistration.entity;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "seller_registration",
	uniqueConstraints = @UniqueConstraint(name = "uk_seller_member", columnNames = "member_id"),
	indexes = {
		@Index(name = "idx_seller_member", columnList = "member_id"),
		@Index(name = "idx_seller_status", columnList = "status")
	})
public class SellerRegistration extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** SELLER 신청한 멤버 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@NotBlank
	@Size(max = 100)
	@Column(name = "store_name", nullable = false, length = 100)
	private String storeName;

	@NotBlank
	@Size(max = 50)
	@Column(name = "business_registration_number", nullable = false, length = 50)
	private String businessRegistrationNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private SellerRegistrationStatus status = SellerRegistrationStatus.APPLIED;

	/** 승인한 관리자(Member) – null 허용 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by_member_id")
	private Member approvedBy;

	@Lob
	@Column(name = "reason")
	private String reason;

	public void approve(Member admin) {
		this.status = SellerRegistrationStatus.APPROVED;
		this.approvedBy = admin;
		this.reason = null;
	}

	public void reject(Member admin, String reason) {
		this.status = SellerRegistrationStatus.REJECTED;
		this.approvedBy = admin;
		this.reason = reason;
	}
}
