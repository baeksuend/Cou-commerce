package com.backsuend.coucommerce.sellerregistration.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.service.AuthorizationService;
import com.backsuend.coucommerce.common.service.MdcLogging;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.sellerregistration.dto.CreateSellerRegistrationRequest;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.backsuend.coucommerce.sellerregistration.repository.SellerRegistrationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerRegistrationService {

	private final MemberRepository memberRepository;
	private final SellerRegistrationRepository sellerRegistrationRepository;
	private final AuthorizationService authorizationService;

	public void apply(Long userId, CreateSellerRegistrationRequest request) {
		/**
		 * 사용자의 판매자 등록을 신청합니다.
		 * -
		 * MDC-CONTEXT:
		 * - 공통 필드: traceId, memberId (이메일), memberRole
		 * - targetMemberId: 신청을 제출하는 사용자 ID
		 * - storeName: 신청하는 상점 이름
		 * - businessRegistrationNumber: 사업자 등록 번호
		 */
		// 현재 인증된 사용자가 본인인지 확인
		authorizationService.authorizeCurrentUser(userId);

		try (var ignored = MdcLogging.withContexts(Map.of(
			"targetMemberId", userId.toString(),
			"storeName", request.storeName(),
			"businessRegistrationNumber", request.businessRegistrationNumber()
		))) {
			Member member = memberRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

			if (member.getRole() != Role.BUYER) {
				throw new BusinessException(ErrorCode.CONFLICT, "판매자 또는 관리자는 판매자 신청을 할 수 없습니다.");
			}

			if (sellerRegistrationRepository.existsByMember(member)) {
				throw new BusinessException(ErrorCode.CONFLICT, "이미 판매자 신청을 했거나 판매자입니다.");
			}

			SellerRegistration sellerRegistration = SellerRegistration.builder()
				.member(member)
				.storeName(request.storeName())
				.businessRegistrationNumber(request.businessRegistrationNumber())
				.build(); // Status defaults to APPLIED

			sellerRegistrationRepository.save(sellerRegistration);
		}
	}
}
