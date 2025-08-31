package com.backsuend.coucommerce.cart.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.repository.MemberRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

@Service
@RequiredArgsConstructor
public class IdService {
	private final MemberRepository memberRepository;

	/** email → memberId 조회 */
	public Long getMemberIdByEmail(String email) {
		return memberRepository.findByEmail(email)
			.map(Member::getId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.ACCESS_DENIED,
				"해당 이메일을 가진 회원이 존재하지 않습니다.",
				Map.of("email", email)
			));
	}
}
