package com.backsuend.coucommerce.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

	private final MemberRepository memberRepository;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public void changeMemberStatus(Long userId, MemberStatus newStatus) {
		Member member = memberRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		// Update status
		member.updateStatus(newStatus);
		memberRepository.save(member);

		// 보안 모범 사례: 상태가 LOCKED 또는 DORMANT로 변경되면 해당 사용자의 모든 리프레시 토큰을 무효화한다.
		if (newStatus == MemberStatus.LOCKED
			|| newStatus == MemberStatus.DORMANT) {
			refreshTokenService.deleteAllTokensForUser(userId);
			log.warn("사용자 {}의 상태가 {}로 변경되었습니다. 이 사용자의 모든 리프레시 토큰이 무효화되었습니다.", userId, newStatus);
		}
	}
}
