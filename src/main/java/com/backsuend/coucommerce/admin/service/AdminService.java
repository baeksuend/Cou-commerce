package com.backsuend.coucommerce.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.admin.dto.SellerApplicationResponse;
import com.backsuend.coucommerce.admin.dto.SellerRejectionRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.seller.entity.Seller;
import com.backsuend.coucommerce.seller.entity.SellerStatus;
import com.backsuend.coucommerce.seller.repository.SellerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

	private final MemberRepository memberRepository;
	private final RefreshTokenService refreshTokenService;
	private final SellerRepository sellerRepository;

	@Transactional
	public void changeMemberStatus(Long userId, com.backsuend.coucommerce.auth.entity.MemberStatus newStatus) {
		Member member = memberRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		// Update status
		member.updateStatus(newStatus);
		memberRepository.save(member);

		// 보안 모범 사례: 상태가 LOCKED 또는 DORMANT로 변경되면 해당 사용자의 모든 리프레시 토큰을 무효화한다.
		if (newStatus == com.backsuend.coucommerce.auth.entity.MemberStatus.LOCKED
			|| newStatus == com.backsuend.coucommerce.auth.entity.MemberStatus.DORMANT) {
			refreshTokenService.deleteAllTokensForUser(userId);
			log.warn("사용자 {}의 상태가 {}로 변경되었습니다. 이 사용자의 모든 리프레시 토큰이 무효화되었습니다.", userId, newStatus);
		}
	}

	@Transactional(readOnly = true)
	public List<SellerApplicationResponse> getPendingSellerApplications() {
		List<Seller> applications = sellerRepository.findByStatus(SellerStatus.APPLIED);
		return applications.stream()
			.map(app -> new SellerApplicationResponse(
				app.getId(),
				app.getMember().getEmail(),
				app.getMember().getName(),
				app.getStoreName(),
				app.getBusinessRegistrationNumber(),
				app.getStatus(),
				app.getCreatedAt()
			))
			.collect(Collectors.toList());
	}

	@Transactional
	public void approveSellerApplication(Long applicationId, Long adminId) {
		Seller application = sellerRepository.findById(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 신청을 찾을 수 없습니다."));
		Member admin = memberRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

		if (application.getStatus() != SellerStatus.APPLIED) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
		}

		Member applicant = application.getMember();
		applicant.updateRole(Role.SELLER);
		memberRepository.save(applicant);

		application.approve(admin);
		sellerRepository.save(application);
	}

	@Transactional
	public void rejectSellerApplication(Long applicationId, Long adminId, SellerRejectionRequest request) {
		Seller application = sellerRepository.findById(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 신청을 찾을 수 없습니다."));
		Member admin = memberRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

		if (application.getStatus() != SellerStatus.APPLIED) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
		}

		application.reject(admin, request.reason());
		sellerRepository.save(application);
	}
}
