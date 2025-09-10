package com.backsuend.coucommerce.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.admin.dto.SellerRegistrationResponse;
import com.backsuend.coucommerce.admin.dto.SellerRejectionRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.sellerregistration.dto.SellerRegistrationSearchRequest;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;
import com.backsuend.coucommerce.sellerregistration.repository.SellerRegistrationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

	private final MemberRepository memberRepository;
	private final RefreshTokenService refreshTokenService;
	private final SellerRegistrationRepository sellerRegistrationRepository;

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

	@Transactional(readOnly = true)
	public Page<SellerRegistrationResponse> searchSellerRegistrations(SellerRegistrationSearchRequest request,
		Pageable pageable) {
		Page<SellerRegistration> registrations = sellerRegistrationRepository.search(request, pageable);
		return registrations.map(reg -> new SellerRegistrationResponse(
			reg.getId(),
			reg.getMember().getEmail(),
			reg.getMember().getName(),
			reg.getStoreName(),
			reg.getBusinessRegistrationNumber(),
			reg.getStatus(),
			reg.getCreatedAt()
		));
	}

	@Transactional
	public void approveSellerRegistration(Long registrationId, Long adminId) {
		SellerRegistration registration = sellerRegistrationRepository.findById(registrationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 신청을 찾을 수 없습니다."));
		Member admin = memberRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

		if (registration.getStatus() != SellerRegistrationStatus.APPLIED) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
		}

		Member applicant = registration.getMember();
		applicant.updateRole(Role.SELLER);
		memberRepository.save(applicant);

		registration.approve(admin);
		sellerRegistrationRepository.save(registration);
	}

	@Transactional
	public void rejectSellerRegistration(Long registrationId, Long adminId, SellerRejectionRequest request) {
		SellerRegistration registration = sellerRegistrationRepository.findById(registrationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 신청을 찾을 수 없습니다."));
		Member admin = memberRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

		if (registration.getStatus() != SellerRegistrationStatus.APPLIED) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
		}

		registration.reject(admin, request.reason());
		sellerRegistrationRepository.save(registration);
	}
}
