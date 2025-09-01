package com.backsuend.coucommerce.admin;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.admin.dto.MemberStatusChangeRequest;
import com.backsuend.coucommerce.admin.dto.SellerRejectionRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.seller.entity.Seller;
import com.backsuend.coucommerce.seller.entity.SellerStatus;
import com.backsuend.coucommerce.seller.repository.SellerRepository;

@DisplayName("관리자 기능 통합 테스트")
public class AdminIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private SellerRepository sellerRepository;

	@Nested
	@DisplayName("회원 상태 변경 API")
	class ChangeMemberStatusApi {

		@Test
		@DisplayName("성공 - 관리자 권한으로 회원 상태를 LOCKED로 변경하고 토큰을 무효화한다")
		@WithMockUser(roles = "ADMIN")
		void changeMemberStatus_success_locked() throws Exception {
			// Given
			Member targetUser = createMember("target@example.com", "targetpass!", Role.BUYER);
			String targetUserRefreshToken = getRefreshToken("target@example.com", "targetpass!");
			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());

			// Then
			Optional<Member> updatedMemberOpt = memberRepository.findById(targetUser.getId());
			assertThat(updatedMemberOpt).isPresent();
			assertThat(updatedMemberOpt.get().getStatus()).isEqualTo(MemberStatus.LOCKED);

			String redisKey = "refreshToken:" + targetUserRefreshToken;
			String storedTokenInfo = redisTemplate.opsForValue().get(redisKey);
			assertThat(storedTokenInfo).isNull();
		}

		@Test
		@DisplayName("성공 - 관리자 권한으로 회원 상태를 ACTIVE로 변경한다 (토큰 무효화 없음)")
		@WithMockUser(roles = "ADMIN")
		void changeMemberStatus_success_active() throws Exception {
			// Given
			Member targetUser = createMember("target2@example.com", "targetpass!", Role.BUYER);
			targetUser.updateStatus(MemberStatus.LOCKED);
			memberRepository.save(targetUser);
			entityManager.flush();

			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.ACTIVE);

			// When
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());

			// Then
			Member updatedMember = findMemberInNewTransaction(targetUser.getId());
			assertThat(updatedMember).isNotNull();
			assertThat(updatedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		}

		@Test
		@DisplayName("실패 - 관리자 권한이 없는 사용자는 회원 상태를 변경할 수 없다")
		void changeMemberStatus_fail_notAdmin() throws Exception {
			// Given
			createMember("buyer@example.com", "buyerpass!", Role.BUYER);
			String buyerAccessToken = login("buyer@example.com", "buyerpass!");
			Member targetUser = createMember("target3@example.com", "targetpass!", Role.BUYER);
			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.header("Authorization", "Bearer " + buyerAccessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("실패 - 인증되지 않은 사용자는 회원 상태를 변경할 수 없다")
		void changeMemberStatus_fail_unauthorized() throws Exception {
			// Given
			Member targetUser = createMember("target4@example.com", "targetpass!", Role.BUYER);
			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 회원의 상태를 변경할 수 없다")
		@WithMockUser(roles = "ADMIN")
		void changeMemberStatus_fail_memberNotFound() throws Exception {
			// Given
			Long nonExistentUserId = 9999L;
			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", nonExistentUserId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 상태 값으로 변경할 수 없다")
		@WithMockUser(roles = "ADMIN")
		void changeMemberStatus_fail_invalidStatusValue() throws Exception {
			// Given
			Member targetUser = createMember("target5@example.com", "targetpass!", Role.BUYER);
			String invalidRequestBody = "{\"newStatus\": \"INVALID_STATUS\"}";

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalidRequestBody))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("판매자 신청 관리 API")
	class SellerApplicationManagementApi {

		private Member applicant;
		private String adminToken;
		private Seller application;

		@BeforeEach
		void setUp() throws Exception {
			// Given
			// 1. 관리자 생성 및 로그인
			createMember("admin@example.com", "adminpass!", Role.ADMIN);
			adminToken = login("admin@example.com", "adminpass!");

			// 2. 판매자 신청자 생성
			applicant = createMember("applicant@example.com", "applicantpass!", Role.BUYER);

			// 3. 판매자 신청 데이터 생성
			application = Seller.builder()
				.member(applicant)
				.storeName("신청자의 상점")
				.businessRegistrationNumber("111-22-33333")
				.status(SellerStatus.APPLIED)
				.build();
			sellerRepository.save(application);
		}

		@Test
		@DisplayName("성공 - 판매자 신청 목록을 조회한다")
		void getSellerApplications_success() throws Exception {
			// When & Then
			mockMvc.perform(get("/api/v1/admin/seller-applications")
					.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].applicationId").value(application.getId()))
				.andExpect(jsonPath("$.data[0].userEmail").value(applicant.getEmail()));
		}

		@Test
		@DisplayName("성공 - 판매자 신청을 승인한다")
		void approveSellerApplication_success() throws Exception {
			// When
			mockMvc.perform(post("/api/v1/admin/seller-applications/{applicationId}/approve", application.getId())
					.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

			// Then
			// 1. 신청 상태가 APPROVED로 변경되었는지 확인
			Seller approvedApplication = sellerRepository.findById(application.getId()).orElseThrow();
			assertThat(approvedApplication.getStatus()).isEqualTo(SellerStatus.APPROVED);

			// 2. 신청자의 역할이 SELLER로 변경되었는지 확인
			Member sellerMember = findMemberInNewTransaction(applicant.getId());
			assertThat(sellerMember.getRole()).isEqualTo(Role.SELLER);
		}

		@Test
		@DisplayName("성공 - 판매자 신청을 거절한다")
		void rejectSellerApplication_success() throws Exception {
			// Given
			SellerRejectionRequest request = new SellerRejectionRequest("서류 부족");

			// When
			mockMvc.perform(post("/api/v1/admin/seller-applications/{applicationId}/reject", application.getId())
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());

			// Then
			// 1. 신청 상태가 REJECTED로 변경되었는지 확인
			Seller rejectedApplication = sellerRepository.findById(application.getId()).orElseThrow();
			assertThat(rejectedApplication.getStatus()).isEqualTo(SellerStatus.REJECTED);
			assertThat(rejectedApplication.getReason()).isEqualTo("서류 부족");

			// 2. 신청자의 역할이 BUYER로 유지되는지 확인
			Member buyerMember = findMemberInNewTransaction(applicant.getId());
			assertThat(buyerMember.getRole()).isEqualTo(Role.BUYER);
		}

		@Test
		@DisplayName("실패 - 일반 사용자는 신청 목록을 조회할 수 없다")
		void getSellerApplications_fail_notAdmin() throws Exception {
			// Given
			String buyerToken = login(applicant.getEmail(), "applicantpass!");

			// When & Then
			mockMvc.perform(get("/api/v1/admin/seller-applications")
					.header("Authorization", "Bearer " + buyerToken))
				.andExpect(status().isForbidden());
		}
	}
}
