package com.backsuend.coucommerce.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.dto.AddressChangeRequest;
import com.backsuend.coucommerce.member.dto.PasswordChangeRequest;
import com.backsuend.coucommerce.member.dto.UserProfileResponse;
import com.backsuend.coucommerce.member.repository.AddressRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final AddressRepository addressRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;

	public UserProfileResponse getUserProfile(Long userId) {
		Member member = memberRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		Address address = addressRepository.findByMember(member)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자의 주소 정보를 찾을 수 없습니다."));

		return new UserProfileResponse(
			member.getEmail(),
			member.getName(),
			member.getPhone(),
			member.getRole(),
			member.getStatus(),
			member.getCreatedAt(),
			address.getPostalCode(),
			address.getRoadName(),
			address.getDetail()
		);
	}

	@Transactional
	public void changeAddress(Long userId, AddressChangeRequest request) {
		Member member = memberRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		Address address = addressRepository.findByMember(member)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자의 주소 정보를 찾을 수 없습니다."));

		address.updateAddress(request.postalCode(), request.roadName(), request.detail());
		addressRepository.save(address);
	}

	@Transactional
	public void changePassword(Long userId, PasswordChangeRequest request) {
		Member member = memberRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		// 이전 비밀번호 확인
		if (!passwordEncoder.matches(request.oldPassword(), member.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "현재 비밀번호가 일치하지 않습니다.");
		}

		// 비밀번호 업데이트
		member.updatePassword(passwordEncoder.encode(request.newPassword()));
		memberRepository.save(member);

		// 보안을 위해 모든 리프레시 토큰 무효화
		refreshTokenService.deleteAllTokensForUser(userId);
		log.info("사용자 {}가 비밀번호를 변경했습니다. 모든 리프레시 토큰이 무효화되었습니다.", userId);
	}
}
