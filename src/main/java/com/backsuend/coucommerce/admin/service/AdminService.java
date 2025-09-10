package com.backsuend.coucommerce.admin.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.backsuend.coucommerce.common.service.AuthorizationService;
import com.backsuend.coucommerce.common.service.MdcLogging;
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

	private static final Logger securityLogger = LoggerFactory.getLogger("securityLogger");

	private final MemberRepository memberRepository;
	private final RefreshTokenService refreshTokenService;
	private final SellerRegistrationRepository sellerRegistrationRepository;
	private final AuthorizationService authorizationService;

	/**
	 * 회원의 상태를 변경합니다. (관리자 권한 필요)
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - targetMemberId: 상태 변경 대상 사용자 ID
	 * - newStatus: 변경될 새로운 상태
	 */
	@Transactional
	public void changeMemberStatus(Long userId, MemberStatus newStatus) {
		Member member = memberRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		// 상태 업데이트
		member.updateStatus(newStatus);
		memberRepository.save(member);

		// 보안 모범 사례: 상태가 LOCKED 또는 DORMANT로 변경되면 해당 사용자의 모든 리프레시 토큰을 무효화한다.
		if (newStatus == MemberStatus.LOCKED
			|| newStatus == MemberStatus.DORMANT) {
			refreshTokenService.deleteAllTokensForUser(userId);
			log.warn("사용자 {}의 상태가 {}로 변경되었습니다. 이 사용자의 모든 리프레시 토큰이 무효화되었습니다.", userId, newStatus);
		}
	}

	/**
	 * 판매자 등록 신청 목록을 검색합니다. (관리자 권한 필요)
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - searchStatus: 검색 필터로 사용된 신청 상태
	 * - adminId: 검색을 수행한 관리자 ID
	 */
	@Transactional(readOnly = true)
	public Page<SellerRegistrationResponse> searchSellerRegistrations(SellerRegistrationSearchRequest request,
		Pageable pageable) {
		// 관리자 권한 확인
		authorizationService.authorizeAdmin();

		try (var ignored = MdcLogging.withContexts(Map.of(
			"searchStatus", request.getStatus() != null ? request.getStatus().name() : "ALL",
			"adminId", authorizationService.getCurrentUser().getId().toString()
		))) {
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
	}

	/**
	 * 판매자 등록 신청을 승인합니다. (관리자 권한 필요)
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - registrationId: 승인된 판매자 등록 신청 ID
	 * - adminId: 승인을 수행한 관리자 ID
	 */
	@Transactional
	public void approveSellerRegistration(Long registrationId, Long adminId) {
		// 관리자 권한 확인
		authorizationService.authorizeAdmin();

		try (var ignored = MdcLogging.withContexts(Map.of(
			"registrationId", registrationId.toString(),
			"adminId", adminId.toString()
		))) {
			RegistrationAndAdmin result = validateAndGetRegistrationAndAdmin(registrationId, adminId);
			SellerRegistration registration = result.registration();
			Member admin = result.admin();

			Member applicant = registration.getMember();
			applicant.updateRole(Role.SELLER);
			memberRepository.save(applicant);

			registration.approve(admin);
			sellerRegistrationRepository.save(registration);
			securityLogger.info("ADMIN_ACTION: 관리자 {}가 판매자 등록 신청 {}을 승인했습니다. 신청자: {}", adminId, registrationId,
				applicant.getEmail());
		}
	}

	/**
	 * 판매자 등록 신청을 반려합니다. (관리자 권한 필요)
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - registrationId: 반려된 판매자 등록 신청 ID
	 * - adminId: 반려를 수행한 관리자 ID
	 * - rejectionReason: 반려 사유
	 */
	@Transactional
	public void rejectSellerRegistration(Long registrationId, Long adminId, SellerRejectionRequest request) {
		// 관리자 권한 확인
		authorizationService.authorizeAdmin();

		try (var ignored = MdcLogging.withContexts(Map.of(
			"registrationId", registrationId.toString(),
			"adminId", adminId.toString(),
			"rejectionReason", request.reason()
		))) {
			RegistrationAndAdmin result = validateAndGetRegistrationAndAdmin(registrationId, adminId);
			SellerRegistration registration = result.registration();
			Member admin = result.admin();

			registration.reject(admin, request.reason());
			sellerRegistrationRepository.save(registration);
			securityLogger.warn("ADMIN_ACTION: 관리자 {}가 판매자 등록 신청 {}을 반려했습니다. 사유: {}", adminId, registrationId,
				request.reason());
		}
	}

	private RegistrationAndAdmin validateAndGetRegistrationAndAdmin(Long registrationId, Long adminId) {
		SellerRegistration registration = sellerRegistrationRepository.findById(registrationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 신청을 찾을 수 없습니다."));
		Member admin = memberRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

		if (registration.getStatus() != SellerRegistrationStatus.APPLIED) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
		}
		return new RegistrationAndAdmin(registration, admin);
	}

	private record RegistrationAndAdmin(SellerRegistration registration, Member admin) {
	}
}
