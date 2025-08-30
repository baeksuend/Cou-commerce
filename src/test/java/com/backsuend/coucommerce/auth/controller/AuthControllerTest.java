package com.backsuend.coucommerce.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.backsuend.coucommerce.auth.dto.AuthResponse;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.RefreshTokenRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.service.AuthService;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockMvc mockMvc;
	@Mock
	private AuthService authService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private AuthController authController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
	}

	@Nested
	@DisplayName("회원가입 API")
	class RegisterApi {

		@Test
		@DisplayName("성공 - 유효한 데이터로 요청 시 201 Created와 함께 토큰을 반환한다")
		void register_withValidData_shouldReturnTokens() throws Exception {
			// Given
			SignupRequest request = new SignupRequest(
				"test@example.com",
				"password123!",
				"Test User",
				"010-1234-5678",
				"12345",
				"Test Road",
				"101"
			);
			AuthResponse response = new AuthResponse("dummyAccessToken", "dummyRefreshToken");

			given(authService.register(any(SignupRequest.class))).willReturn(response);

			// When & Then
			mockMvc.perform(post("/api/v1/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("dummyAccessToken"))
				.andExpect(jsonPath("$.data.refreshToken").value("dummyRefreshToken"));
		}
	}

	@Nested
	@DisplayName("로그인 API")
	class LoginApi {

		@Test
		@DisplayName("성공 - 유효한 정보로 요청 시 200 OK와 함께 토큰을 반환한다")
		void login_withValidCredentials_shouldReturnTokens() throws Exception {
			// Given
			LoginRequest request = new LoginRequest("test@example.com", "password123!");
			AuthResponse response = new AuthResponse("dummyAccessToken", "dummyRefreshToken");
			given(authService.login(any(LoginRequest.class))).willReturn(response);

			// When & Then
			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("dummyAccessToken"));
		}
	}

	@Nested
	@DisplayName("토큰 재발급 API")
	class RefreshApi {

		@Test
		@DisplayName("성공 - 유효한 리프레시 토큰으로 요청 시 200 OK와 함께 새 토큰을 반환한다")
		void refresh_withValidToken_shouldReturnNewTokens() throws Exception {
			// Given
			RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
			AuthResponse response = new AuthResponse("newAccessToken", "newRefreshToken");
			given(authService.refreshAccessToken(anyString())).willReturn(response);

			// When & Then
			mockMvc.perform(post("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("newAccessToken"));
		}
	}

	@Nested
	@DisplayName("로그아웃 API")
	class LogoutApi {

		@Test
		@DisplayName("성공 - 요청 시 204 No Content를 반환한다")
		void logout_withValidToken_shouldReturnNoContent() throws Exception {
			// Given
			RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
			willDoNothing().given(refreshTokenService).deleteByToken(anyString());

			// When & Then
			mockMvc.perform(post("/api/v1/auth/logout")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());
		}
	}
}
