package com.backsuend.coucommerce.sellerregistration.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
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

	public void apply(Long userId, CreateSellerRegistrationRequest request) {
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
