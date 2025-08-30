package com.backsuend.coucommerce.admin;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.TestTransaction;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.admin.dto.MemberStatusChangeRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;

@DisplayName("관리자 기능 통합 테스트")
public class AdminIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Nested
	@DisplayName("회원 상태 변경 API")
	class ChangeMemberStatusApi {

		@Test
		@DisplayName("성공 - 관리자 권한으로 회원 상태를 LOCKED로 변경하고 토큰을 무효화한다")
		@WithMockUser(roles = "ADMIN")
			// ADMIN 역할로 Mock 사용자 설정
		void changeMemberStatus_success_locked() throws Exception {
			// Given
			// 1. 관리자 사용자 생성 및 로그인 (이 테스트에서는 @WithMockUser로 대체되므로 주석 처리하거나 제거 가능)
			// Member adminUser = createMember("admin@example.com", "adminpass!", Role.ADMIN);
			// String adminAccessToken = login("admin@example.com", "adminpass!");

			// 2. 상태를 변경할 일반 사용자 생성
			Member targetUser = createMember("target@example.com", "targetpass!", Role.BUYER);
			// 대상 사용자의 리프레시 토큰을 미리 생성하여 Redis에 저장되도록 함
			String targetUserRefreshToken = getRefreshToken("target@example.com", "targetpass!");

			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					// .header("Authorization", "Bearer " + adminAccessToken) // @WithMockUser 사용 시 불필요
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());

			// Then
			// 1. DB에서 사용자 상태가 변경되었는지 확인
			Optional<Member> updatedMemberOpt = memberRepository.findById(targetUser.getId());
			assertThat(updatedMemberOpt).isPresent();
			assertThat(updatedMemberOpt.get().getStatus()).isEqualTo(MemberStatus.LOCKED);

			// 2. Redis에서 대상 사용자의 리프레시 토큰이 무효화되었는지 확인
			String redisKey = "refreshToken:" + targetUserRefreshToken;
			String storedTokenInfo = redisTemplate.opsForValue().get(redisKey);
			assertThat(storedTokenInfo).isNull(); // 토큰이 삭제되었으므로 null이어야 함
		}

		@Test
		@DisplayName("성공 - 관리자 권한으로 회원 상태를 ACTIVE로 변경한다 (토큰 무효화 없음)")
		@WithMockUser(roles = "ADMIN") // ADMIN 역할로 Mock 사용자 설정
		@Rollback(false)
			// Prevent rollback to see committed changes
		void changeMemberStatus_success_active() throws Exception {
			// Given
			// 1. 관리자 사용자 생성 및 로그인 (이 테스트에서는 @WithMockUser로 대체되므로 주석 처리하거나 제거 가능)
			// Member adminUser = createMember("admin2@example.com", "adminpass!", Role.ADMIN);
			// String adminAccessToken = login("admin2@example.com", "adminpass!");

			// 2. 상태를 변경할 일반 사용자 생성 (초기 상태를 LOCKED로 설정)
			Member targetUser = createMember("target2@example.com", "targetpass!", Role.BUYER);
			targetUser.updateStatus(MemberStatus.LOCKED);
			memberRepository.save(targetUser);
			entityManager.flush(); // Ensure changes are persisted before the HTTP request

			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.ACTIVE);

			// When
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					// .header("Authorization", "Bearer " + adminAccessToken) // @WithMockUser 사용 시 불필요
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());

			// Then
			// DB에서 사용자 상태가 변경되었는지 확인
			Member updatedMember = findMemberInNewTransaction(targetUser.getId());
			assertThat(updatedMember).isNotNull();
			assertThat(updatedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		}

		@Test
		@DisplayName("실패 - 관리자 권한이 없는 사용자는 회원 상태를 변경할 수 없다")
		void changeMemberStatus_fail_notAdmin() throws Exception {
			// Given
			// 1. 일반 사용자 생성 및 로그인
			Member buyerUser = createMember("buyer@example.com", "buyerpass!", Role.BUYER);
			String buyerAccessToken = login("buyer@example.com", "buyerpass!");

			// 2. 상태를 변경할 대상 사용자 생성
			Member targetUser = createMember("target3@example.com", "targetpass!", Role.BUYER);

			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.header("Authorization", "Bearer " + buyerAccessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden()); // 403 Forbidden
		}

		@Test
		@DisplayName("실패 - 인증되지 않은 사용자는 회원 상태를 변경할 수 없다")
		void changeMemberStatus_fail_unauthorized() throws Exception {
			// Given
			// 상태를 변경할 대상 사용자 생성
			Member targetUser = createMember("target4@example.com", "targetpass!", Role.BUYER);

			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized()); // 401 Unauthorized
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 회원의 상태를 변경할 수 없다")
		@WithMockUser(roles = "ADMIN")
			// ADMIN 역할로 Mock 사용자 설정
		void changeMemberStatus_fail_memberNotFound() throws Exception {
			// Given
			Long nonExistentUserId = 9999L; // 존재하지 않는 사용자 ID
			MemberStatusChangeRequest request = new MemberStatusChangeRequest(MemberStatus.LOCKED);

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", nonExistentUserId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound()); // 404 Not Found
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 상태 값으로 변경할 수 없다")
		void changeMemberStatus_fail_invalidStatusValue() throws Exception {
			// Given
			// 1. 관리자 사용자 생성 및 로그인
			Member adminUser = createMember("admin4@example.com", "adminpass!", Role.ADMIN);
			String adminAccessToken = login("admin4@example.com", "adminpass!");

			// 2. 상태를 변경할 일반 사용자 생성
			Member targetUser = createMember("target5@example.com", "targetpass!", Role.BUYER);

			// 유효하지 않은 상태 값 (Enum에 없는 값)
			String invalidRequestBody = "{\"newStatus\": \"INVALID_STATUS\"}";

			// When & Then
			mockMvc.perform(put("/api/v1/admin/members/{userId}/status", targetUser.getId())
					.header("Authorization", "Bearer " + adminAccessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalidRequestBody))
				.andExpect(status().isBadRequest()); // 400 Bad Request
		}
	}
}