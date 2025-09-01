package com.backsuend.coucommerce.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
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
@Table(name = "member",
	indexes = {
		@Index(name = "idx_member_email", columnList = "email", unique = true),
		@Index(name = "idx_member_phone", columnList = "phone")
	})
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Member extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Email
	@NotBlank
	@Size(max = 200)
	@Column(name = "email", nullable = false, length = 200, unique = true)
	private String email;

	@NotBlank
	@Size(min = 8, max = 100)
	@Column(name = "password", nullable = false, length = 100)
	private String password; // 해시 저장

	@NotBlank
	@Size(max = 50)
	@Column(name = "phone", nullable = false, length = 50)
	private String phone;

	@NotBlank
	@Size(max = 50)
	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private MemberStatus status = MemberStatus.ACTIVE;

	public void updatePassword(String newPassword) {
		this.password = newPassword;
	}

	public void updateStatus(MemberStatus newStatus) {
		this.status = newStatus;
	}

	public void updateRole(Role newRole) {
		this.role = newRole;
	}

}
