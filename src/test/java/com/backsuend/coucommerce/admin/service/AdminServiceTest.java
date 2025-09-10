package com.backsuend.coucommerce.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.backsuend.coucommerce.sellerregistration.dto.SellerRegistrationSearchRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backsuend.coucommerce.admin.dto.SellerRegistrationResponse;
import com.backsuend.coucommerce.admin.dto.SellerRejectionRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;
import com.backsuend.coucommerce.sellerregistration.repository.SellerRegistrationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 단위 테스트")
class AdminServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private SellerRegistrationRepository sellerRegistrationRepository;

	@InjectMocks
	private AdminService adminService;

	private Member member;
	private Member admin;

	@BeforeEach
	void setUp() {
		member = Member.builder()
			.id(1L)
			.email("test@example.com")
			.password("encodedPassword")
			.name("Test User")
			.phone("010-1234-5678")
			.role(Role.BUYER)
			.status(MemberStatus.ACTIVE)
			.build();

		admin = Member.builder()
			.id(99L)
			.email("admin@example.com")
			.role(Role.ADMIN)
			.build();
	}

	@Nested
	@DisplayName("회원 상태 변경")
	class ChangeMemberStatus {

		@Test
		@DisplayName("성공 - 상태가 ACTIVE로 변경될 때 토큰 무효화가 발생하지 않는다")
		void changeMemberStatus_success_active() {
			// Given
			Long userId = 1L;
			MemberStatus newStatus = MemberStatus.ACTIVE;
			when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

			// When
			adminService.changeMemberStatus(userId, newStatus);

			// Then
			verify(memberRepository, times(1)).findById(userId);
			assertThat(member.getStatus()).isEqualTo(newStatus);
			verify(memberRepository, times(1)).save(member);
			verify(refreshTokenService, never()).deleteAllTokensForUser(anyLong());
		}

		@Test
		@DisplayName("성공 - 상태가 LOCKED로 변경될 때 토큰 무효화가 발생한다")
		void changeMemberStatus_success_locked() {
			// Given
			Long userId = 1L;
			MemberStatus newStatus = MemberStatus.LOCKED;
			when(memberRepository.findById(userId)).thenReturn(Optional.of(member));
			doNothing().when(refreshTokenService).deleteAllTokensForUser(userId);

			// When
			adminService.changeMemberStatus(userId, newStatus);

			// Then
			verify(memberRepository, times(1)).findById(userId);
			assertThat(member.getStatus()).isEqualTo(newStatus);
			verify(memberRepository, times(1)).save(member);
			verify(refreshTokenService, times(1)).deleteAllTokensForUser(userId);
		}

		@Test
		@DisplayName("성공 - 상태가 DORMANT로 변경될 때 토큰 무효화가 발생한다")
		void changeMemberStatus_success_dormant() {
			// Given
			Long userId = 1L;
			MemberStatus newStatus = MemberStatus.DORMANT;
			when(memberRepository.findById(userId)).thenReturn(Optional.of(member));
			doNothing().when(refreshTokenService).deleteAllTokensForUser(userId);

			// When
			adminService.changeMemberStatus(userId, newStatus);

			// Then
			verify(memberRepository, times(1)).findById(userId);
			assertThat(member.getStatus()).isEqualTo(newStatus);
			verify(memberRepository, times(1)).save(member);
			verify(refreshTokenService, times(1)).deleteAllTokensForUser(userId);
		}

		@Test
		@DisplayName("실패 - 회원을 찾을 수 없을 때 BusinessException이 발생한다")
		void changeMemberStatus_fail_memberNotFound() {
			// Given
			Long userId = 99L;
			MemberStatus newStatus = MemberStatus.LOCKED;
			when(memberRepository.findById(userId)).thenReturn(Optional.empty());

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class, () ->
				adminService.changeMemberStatus(userId, newStatus)
			);

			assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
			verify(memberRepository, times(1)).findById(userId);
			verify(memberRepository, never()).save(any(Member.class));
			verify(refreshTokenService, never()).deleteAllTokensForUser(anyLong());
		}

		@Test
		@DisplayName("성공 - 현재 상태와 동일한 ACTIVE로 변경 시 토큰 무효화가 발생하지 않는다")
		void changeMemberStatus_success_sameStatusActive() {
			// Given
			Long userId = 1L;
			MemberStatus newStatus = MemberStatus.ACTIVE;
			member.updateStatus(MemberStatus.ACTIVE); // Ensure initial status is ACTIVE
			when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

			// When
			adminService.changeMemberStatus(userId, newStatus);

			// Then
			verify(memberRepository, times(1)).findById(userId);
			assertThat(member.getStatus()).isEqualTo(newStatus);
			verify(memberRepository, times(1)).save(member);
			verify(refreshTokenService, never()).deleteAllTokensForUser(anyLong());
		}

		@Test
		@DisplayName("성공 - 현재 상태와 동일한 LOCKED로 변경 시 토큰 무효화가 발생한다")
		void changeMemberStatus_success_sameStatusLocked() {
			// Given
			Long userId = 1L;
			MemberStatus newStatus = MemberStatus.LOCKED;
			member.updateStatus(MemberStatus.LOCKED); // Ensure initial status is LOCKED
			when(memberRepository.findById(userId)).thenReturn(Optional.of(member));
			doNothing().when(refreshTokenService).deleteAllTokensForUser(userId);

			// When
			adminService.changeMemberStatus(userId, newStatus);

			// Then
			verify(memberRepository, times(1)).findById(userId);
			assertThat(member.getStatus()).isEqualTo(newStatus);
			verify(memberRepository, times(1)).save(member);
			verify(refreshTokenService, times(1)).deleteAllTokensForUser(userId);
		}

		@Test
		@DisplayName("성공 - 현재 상태와 동일한 DORMANT로 변경 시 토큰 무효화가 발생한다")
		void changeMemberStatus_success_sameStatusDormant() {
			// Given
			Long userId = 1L;
			MemberStatus newStatus = MemberStatus.DORMANT;
			member.updateStatus(MemberStatus.DORMANT); // Ensure initial status is DORMANT
			when(memberRepository.findById(userId)).thenReturn(Optional.of(member));
			doNothing().when(refreshTokenService).deleteAllTokensForUser(userId);

			// When
			adminService.changeMemberStatus(userId, newStatus);

			// Then
			verify(memberRepository, times(1)).findById(userId);
			assertThat(member.getStatus()).isEqualTo(newStatus);
			verify(memberRepository, times(1)).save(member);
			verify(refreshTokenService, times(1)).deleteAllTokensForUser(userId);
		}
	}

	@Nested
	@DisplayName("판매자 등록 신청 관리")
	class ManageSellerRegistration {

		private SellerRegistration registration;

		@BeforeEach
		void setUp() {
			registration = SellerRegistration.builder()
				.id(1L)
				.member(member)
				.storeName("테스트 상점")
				.businessRegistrationNumber("123-45-67890")
				.status(SellerRegistrationStatus.APPLIED)
				.build();
		}

		@Test
		@DisplayName("성공 - 판매자 등록 신청 목록 검색 및 페이징 조회")
		void searchSellerRegistrations_success() {
			// given
			SellerRegistrationSearchRequest searchRequest = SellerRegistrationSearchRequest.builder()
				.status(SellerRegistrationStatus.APPLIED)
				.build();
			Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
			Page<SellerRegistration> page = new PageImpl<>(List.of(registration), pageable, 1);

			when(sellerRegistrationRepository.search(searchRequest, pageable)).thenReturn(page);

			// when
			Page<SellerRegistrationResponse> responses = adminService.searchSellerRegistrations(searchRequest, pageable);

			// then
			assertThat(responses.getContent()).hasSize(1);
			SellerRegistrationResponse response = responses.getContent().getFirst();
			assertThat(response.registrationId()).isEqualTo(registration.getId());
			assertThat(response.userEmail()).isEqualTo(member.getEmail());
			assertThat(response.storeName()).isEqualTo(registration.getStoreName());
			verify(sellerRegistrationRepository, times(1)).search(searchRequest, pageable);
		}

		@Test
		@DisplayName("성공 - 판매자 등록 신청 승인")
		void approveSellerRegistration_success() {
			// given
			when(sellerRegistrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
			when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

			// when
			adminService.approveSellerRegistration(registration.getId(), admin.getId());

			// then
			assertThat(registration.getStatus()).isEqualTo(SellerRegistrationStatus.APPROVED);
			assertThat(registration.getApprovedBy()).isEqualTo(admin);
			assertThat(member.getRole()).isEqualTo(Role.SELLER);
			verify(sellerRegistrationRepository, times(1)).save(registration);
			verify(memberRepository, times(1)).save(member);
		}

		@Test
		@DisplayName("실패 - 이미 처리된 신청을 승인")
		void approveSellerRegistration_fail_alreadyProcessed() {
			// given
			registration.approve(admin); // 이미 승인된 상태로 변경
			when(sellerRegistrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
			when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

			// when & then
			BusinessException exception = assertThrows(BusinessException.class, () -> {
				adminService.approveSellerRegistration(registration.getId(), admin.getId());
			});
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
		}

		@Test
		@DisplayName("성공 - 판매자 등록 신청 거절")
		void rejectSellerRegistration_success() {
			// given
			SellerRejectionRequest request = new SellerRejectionRequest("서류 미비");
			when(sellerRegistrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
			when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

			// when
			adminService.rejectSellerRegistration(registration.getId(), admin.getId(), request);

			// then
			assertThat(registration.getStatus()).isEqualTo(SellerRegistrationStatus.REJECTED);
			assertThat(registration.getApprovedBy()).isEqualTo(admin);
			assertThat(registration.getReason()).isEqualTo(request.reason());
			assertThat(member.getRole()).isEqualTo(Role.BUYER); // 역할은 변경되지 않음
			verify(sellerRegistrationRepository, times(1)).save(registration);
			verify(memberRepository, never()).save(member);
		}
	}
}
