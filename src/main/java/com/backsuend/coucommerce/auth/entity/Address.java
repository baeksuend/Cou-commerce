package com.backsuend.coucommerce.auth.entity;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author rua
 */

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "address", indexes = @Index(name = "idx_address_member", columnList = "member_id", unique = true))
public class Address extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// member와 1:1
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@NotBlank
	@Size(max = 10)
	@Column(name = "postal_code", nullable = false, length = 10)
	private String postalCode;

	@NotBlank
	@Size(max = 100)
	@Column(name = "road_name", nullable = false, length = 100)
	private String roadName;

	@NotBlank
	@Size(max = 50)
	@Column(name = "detail", nullable = false, length = 50)
	private String detail;

	public void updateAddress(String postalCode, String roadName, String detail) {
		this.postalCode = postalCode;
		this.roadName = roadName;
		this.detail = detail;
	}
}
