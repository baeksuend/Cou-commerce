package com.backsuend.coucommerce.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.filter.RateLimitingFilter;
import com.backsuend.coucommerce.member.dto.AddressChangeRequest;
import com.backsuend.coucommerce.member.dto.PasswordChangeRequest;
import com.backsuend.coucommerce.member.dto.UserProfileResponse;
import com.backsuend.coucommerce.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = MemberController.class,
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitingFilter.class)
	})
class MemberControllerTest {

	// @TestConfiguration을 통해 Bean으로 등록된 Mock 객체를 주입받는다.
	@Autowired
	private MemberService memberService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private Authentication authentication;
	private UserDetailsImpl testUserDetails;

	@BeforeEach
	void setUp() {
		// 각 테스트 실행 전에 Mock 객체의 상태(호출 기록 등)를 초기화
		reset(memberService);

		// 테스트용 인증 객체 생성
		Long userId = 1L;
		String username = "test@example.com";
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.BUYER.name()));
		testUserDetails = new UserDetailsImpl(userId, username, "password", authorities, true, true);
		authentication = new UsernamePasswordAuthenticationToken(testUserDetails, null, authorities);
	}

	@Test
	@DisplayName("내 프로필 조회 성공")
	void getProfile_success() throws Exception {
		// given
		Long userId = testUserDetails.getId();
		UserProfileResponse mockResponse = new UserProfileResponse(
			"test@example.com", "테스트 사용자", "010-1234-5678",
			Role.BUYER, MemberStatus.ACTIVE, LocalDateTime.now(),
			"12345", "테스트 도로명", "101호"
		);

		when(memberService.getUserProfile(userId)).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(get("/api/v1/members/me")
				.with(authentication(authentication))) // Spring Security Test를 사용하여 인증 정보 제공
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.email").value(mockResponse.email()))
			.andExpect(jsonPath("$.data.name").value(mockResponse.name()));

		// verify
		verify(memberService, times(1)).getUserProfile(userId);
	}

	@Test
	@DisplayName("내 프로필 조회 실패 - 사용자를 찾을 수 없음")
	void getProfile_fail_userNotFound() throws Exception {
		// given
		Long userId = testUserDetails.getId();
		when(memberService.getUserProfile(userId))
			.thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		// when & then
		mockMvc.perform(get("/api/v1/members/me")
				.with(authentication(authentication)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value(ErrorCode.NOT_FOUND.name()));

		// verify
		verify(memberService, times(1)).getUserProfile(userId);
	}

	@Test
	@DisplayName("주소 변경 성공")
	void changeAddress_success() throws Exception {
		// given
		Long userId = testUserDetails.getId();
		AddressChangeRequest request = new AddressChangeRequest("54321", "새로운 도로명", "202호");

		doNothing().when(memberService).changeAddress(eq(userId), any(AddressChangeRequest.class));

		// when & then
		mockMvc.perform(put("/api/v1/members/me/address")
				.with(authentication(authentication))
				.with(csrf()) // CSRF 토큰 추가
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());

		// verify
		verify(memberService, times(1)).changeAddress(eq(userId), any(AddressChangeRequest.class));
	}

	@Test
	@DisplayName("비밀번호 변경 성공")
	void changePassword_success() throws Exception {
		// given
		Long userId = testUserDetails.getId();
		PasswordChangeRequest request = new PasswordChangeRequest("oldPassword", "newPassword123!");

		doNothing().when(memberService).changePassword(eq(userId), any(PasswordChangeRequest.class));

		// when & then
		mockMvc.perform(post("/api/v1/members/me/password-change")
				.with(authentication(authentication))
				.with(csrf()) // CSRF 토큰 추가
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());

		// verify
		verify(memberService, times(1)).changePassword(eq(userId), any(PasswordChangeRequest.class));
	}

	@Test
	@DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
	void changePassword_fail_passwordMismatch() throws Exception {
		// given
		Long userId = testUserDetails.getId();
		PasswordChangeRequest request = new PasswordChangeRequest("wrongOldPassword", "newPassword123!");

		doThrow(new BusinessException(ErrorCode.INVALID_INPUT, "현재 비밀번호가 일치하지 않습니다."))
			.when(memberService).changePassword(eq(userId), any(PasswordChangeRequest.class));

		// when & then
		mockMvc.perform(post("/api/v1/members/me/password-change")
				.with(authentication(authentication))
				.with(csrf()) // CSRF 토큰 추가
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT.name()));

		// verify
		verify(memberService, times(1)).changePassword(eq(userId), any(PasswordChangeRequest.class));
	}

	/**
	 * @MockBean을 대체하기 위한 테스트 전용 설정 클래스.
	 * 테스트에 필요한 Mock 객체를 Bean으로 직접 등록한다.
	 */
	@TestConfiguration
	static class MemberControllerTestConfig {
		@Bean
		public MemberService memberService() {
			return mock(MemberService.class);
		}
	}
}
