package com.backsuend.coucommerce.member;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.member.dto.AddressChangeRequest;
import com.backsuend.coucommerce.member.dto.PasswordChangeRequest;
import com.backsuend.coucommerce.member.repository.AddressRepository;

@DisplayName("회원 기능 통합 테스트")
public class MemberIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private AddressRepository addressRepository;

	@Nested
	@DisplayName("프로필 조회 API")
	class GetProfileApi {

		@Test
		@DisplayName("성공 - 인증된 사용자는 자신의 프로필을 조회할 수 있다")
		void getProfile_success() throws Exception {
			// Given
			String email = "profileuser@example.com";
			String password = "password123!";
			String name = "프로필유저";
			String phone = "010-1111-2222";
			String postalCode = "04538";
			String roadName = "서울특별시 중구 세종대로 110";
			String detail = "101호";

			// 회원가입 및 로그인하여 토큰 획득
			String accessToken = registerAndLogin(email, password, name, phone);

			// When & Then
			mockMvc.perform(get("/api/v1/members/me")
					.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.name").value(name))
				.andExpect(jsonPath("$.data.phone").value(phone))
				.andExpect(jsonPath("$.data.postalCode").value(postalCode))
				.andExpect(jsonPath("$.data.roadName").value(roadName))
				.andExpect(jsonPath("$.data.detail").value(detail));
		}

		@Test
		@DisplayName("실패 - 인증되지 않은 사용자는 프로필을 조회할 수 없다")
		void getProfile_fail_unauthorized() throws Exception {
			// Given
			// 토큰 없음

			// When & Then
			mockMvc.perform(get("/api/v1/members/me"))
				.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("주소 변경 API")
	class ChangeAddressApi {

		@Test
		@DisplayName("성공 - 인증된 사용자는 주소를 변경할 수 있다")
		void changeAddress_success() throws Exception {
			// Given
			String email = "addresschange@example.com";
			String password = "password123!";
			String accessToken = registerAndLogin(email, password, "주소변경유저", "010-9999-0000");

			AddressChangeRequest request = new AddressChangeRequest("00000", "새로운 도로명 주소", "새로운 상세 주소");

			// When
			mockMvc.perform(put("/api/v1/members/me/address")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.success").value(true));

			// Then - DB에서 주소가 변경되었는지 확인
			Member member = memberRepository.findByEmail(email).orElseThrow();
			com.backsuend.coucommerce.auth.entity.Address updatedAddress = addressRepository.findByMember(member)
				.orElseThrow();
			assertThat(updatedAddress.getPostalCode()).isEqualTo(request.postalCode());
			assertThat(updatedAddress.getRoadName()).isEqualTo(request.roadName());
			assertThat(updatedAddress.getDetail()).isEqualTo(request.detail());
		}

		@Test
		@DisplayName("실패 - 인증되지 않은 사용자는 주소를 변경할 수 없다")
		void changeAddress_fail_unauthorized() throws Exception {
			// Given
			AddressChangeRequest request = new AddressChangeRequest("00000", "새로운 도로명 주소", "새로운 상세 주소");

			// When & Then
			mockMvc.perform(put("/api/v1/members/me/address")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 입력값으로 주소 변경 시 400 Bad Request를 반환한다")
		void changeAddress_fail_validation() throws Exception {
			// Given
			String email = "addressval@example.com";
			String password = "password123!";
			String accessToken = registerAndLogin(email, password, "주소유효성유저", "010-1212-3434");

			// 우편번호가 비어있는 유효하지 않은 요청
			AddressChangeRequest request = new AddressChangeRequest("", "New Road", "202");

			// When & Then
			mockMvc.perform(put("/api/v1/members/me/address")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("비밀번호 변경 API")
	class ChangePasswordApi {

		@Test
		@DisplayName("성공 - 인증된 사용자는 비밀번호를 변경할 수 있다")
		void changePassword_success() throws Exception {
			// Given
			String email = "passwordchange@example.com";
			String oldPassword = "oldPassword123!";
			String newPassword = "newPassword123!";
			String accessToken = registerAndLogin(email, oldPassword, "비번변경유저", "010-3333-4444");

			PasswordChangeRequest request = new PasswordChangeRequest(oldPassword, newPassword);

			// When
			mockMvc.perform(post("/api/v1/members/me/password-change")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.success").value(true));

			// Then - DB에서 비밀번호가 변경되었는지 확인
			Member member = memberRepository.findByEmail(email).orElseThrow();
			assertThat(passwordEncoder.matches(newPassword, member.getPassword())).isTrue();
		}

		@Test
		@DisplayName("실패 - 인증되지 않은 사용자는 비밀번호를 변경할 수 없다")
		void changePassword_fail_unauthorized() throws Exception {
			// Given
			PasswordChangeRequest request = new PasswordChangeRequest("oldPassword", "newPassword");

			// When & Then
			mockMvc.perform(post("/api/v1/members/me/password-change")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 입력값으로 비밀번호 변경 시 400 Bad Request를 반환한다")
		void changePassword_fail_validation() throws Exception {
			// Given
			String email = "validation@example.com";
			String password = "password123!";
			String accessToken = registerAndLogin(email, password, "유효성유저", "010-5555-6666");

			// 새 비밀번호가 비어있는 유효하지 않은 요청
			PasswordChangeRequest request = new PasswordChangeRequest(password, "");

			// When & Then
			mockMvc.perform(post("/api/v1/members/me/password-change")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - 현재 비밀번호가 일치하지 않으면 400 Bad Request를 반환한다")
		void changePassword_fail_wrongOldPassword() throws Exception {
			// Given
			String email = "wrongpass@example.com";
			String oldPassword = "oldPassword123!";
			String accessToken = registerAndLogin(email, oldPassword, "틀린비번유저", "010-7777-8888");

			// 잘못된 현재 비밀번호로 요청
			PasswordChangeRequest request = new PasswordChangeRequest("incorrectOldPassword", "newPassword123!");

			// When & Then
			mockMvc.perform(post("/api/v1/members/me/password-change")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}
	}
}
