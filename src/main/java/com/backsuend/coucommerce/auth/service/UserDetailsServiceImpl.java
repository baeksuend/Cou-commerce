package com.backsuend.coucommerce.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

	private final MemberRepository memberRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("이메일을 찾을 수 없습니다: " + email));

		return UserDetailsImpl.build(member);
	}

	public UserDetailsImpl getUserDetailsByEmail(String email) {
		return (UserDetailsImpl)loadUserByUsername(email);
	}
}
