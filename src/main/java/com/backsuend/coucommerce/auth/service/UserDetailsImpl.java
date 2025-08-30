package com.backsuend.coucommerce.auth.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

public class UserDetailsImpl implements UserDetails {
	@Getter
	private final Long id;
	private final String username; // email

	@JsonIgnore
	private final String password;

	private final Collection<? extends GrantedAuthority> authorities;

	private final boolean isEnabled;
	private final boolean isAccountNonLocked;

	public UserDetailsImpl(Long id, String username, String password,
		Collection<? extends GrantedAuthority> authorities,
		boolean isEnabled, boolean isAccountNonLocked) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.authorities = authorities;
		this.isEnabled = isEnabled;
		this.isAccountNonLocked = isAccountNonLocked;
	}

	/**
	 * Member 엔티티를 기반으로 Spring Security의 UserDetails 객체를 생성한다.
	 * Member의 역할(Role)은 권한(GrantedAuthority)으로, 상태(MemberStatus)는 계정 활성화/잠금 상태로 매핑된다.
	 */
	public static UserDetailsImpl build(Member member) {
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(member.getRole().name()));

		return new UserDetailsImpl(
			member.getId(),
			member.getEmail(),
			member.getPassword(),
			authorities,
			member.getStatus() == MemberStatus.ACTIVE, // 계정 활성 상태 (ACTIVE일 때만 true)
			member.getStatus() != MemberStatus.LOCKED // 계정 잠금 상태 (LOCKED가 아닐 때만 true)
		);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // 요구사항에 따라 추가 구현 가능
	}

	@Override
	public boolean isAccountNonLocked() {
		return this.isAccountNonLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true; // 요구사항에 따라 추가 구현 가능
	}

	@Override
	public boolean isEnabled() {
		return this.isEnabled;
	}
}
