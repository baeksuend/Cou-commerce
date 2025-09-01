package com.backsuend.coucommerce.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.backsuend.coucommerce.auth.dto.AuthResponse;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.jwt.JwtProvider;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.AddressRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtProvider jwtProvider;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private UserDetailsServiceImpl userDetailsService;

	@InjectMocks
	private AuthService authService;

	private SignupRequest signupRequest;
	private Member member;

	@BeforeEach
	void setUp() {
		signupRequest = new SignupRequest(
			"test@example.com",
			"password123",
			"Test User",
			"010-1234-5678",
			"12345",
			"Test Road",
			"101"
		);

		member = Member.builder()
			.id(1L)
			.email(signupRequest.email())
			.password("encodedPassword")
			.name(signupRequest.name())
			.phone(signupRequest.phone())
			.role(Role.BUYER)
			.status(com.backsuend.coucommerce.auth.entity.MemberStatus.ACTIVE)
			.build();
	}

	@Nested
	@DisplayName("회원가입")
	class RegisterTests {

		@Test
		@DisplayName("성공 - 새로운 사용자는 성공적으로 등록되고 토큰을 발급받는다")
		void register_withNewUser_shouldSucceedAndReturnTokens() {
			// Given
			when(memberRepository.findByEmail(signupRequest.email())).thenReturn(Optional.empty());
			when(passwordEncoder.encode(signupRequest.password())).thenReturn("encodedPassword");
			when(memberRepository.save(any(Member.class))).thenReturn(member);
			when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

			UserDetailsImpl userDetails = UserDetailsImpl.build(member);
			Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());
			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
				authentication);

			when(jwtProvider.createAccessToken(anyString(), any(Role.class))).thenReturn("dummyAccessToken");
			when(refreshTokenService.createRefreshToken(anyString(), anyLong())).thenReturn("dummyRefreshToken");

			// When
			AuthResponse authResponse = authService.register(signupRequest);

			// Then
			assertThat(authResponse).isNotNull();
			assertThat(authResponse.accessToken()).isEqualTo("dummyAccessToken");
			assertThat(authResponse.refreshToken()).isEqualTo("dummyRefreshToken");

			verify(memberRepository, times(1)).save(any(Member.class));
			verify(addressRepository, times(1)).save(any(Address.class));
			verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
		}

		@Test
		@DisplayName("실패 - 이미 존재하는 이메일로 등록 시 BusinessException 발생")
		void register_withExistingEmail_shouldThrowBusinessException() {
			// Given
			when(memberRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(member));

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> authService.register(signupRequest));

			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT);
			verify(memberRepository, never()).save(any(Member.class));
		}
	}

	@Nested
	@DisplayName("로그인")
	class LoginTests {

		@Test
		@DisplayName("성공 - 정확한 정보로 로그인 시 토큰을 발급한다")
		void login_withValidCredentials_shouldReturnTokens() {
			// Given
			LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
			UserDetailsImpl userDetails = UserDetailsImpl.build(member);
			Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());

			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
				authentication);
			when(jwtProvider.createAccessToken(anyString(), any(Role.class))).thenReturn("dummyAccessToken");
			when(refreshTokenService.createRefreshToken(anyString(), anyLong())).thenReturn("dummyRefreshToken");

			// When
			AuthResponse authResponse = authService.login(loginRequest);

			// Then
			assertThat(authResponse).isNotNull();
			assertThat(authResponse.accessToken()).isEqualTo("dummyAccessToken");
		}

		@Test
		@DisplayName("실패 - 잘못된 비밀번호로 로그인 시 BadCredentialsException 발생")
		void login_withInvalidCredentials_shouldThrowException() {
			// Given
			LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

			// When & Then
			assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
			verify(jwtProvider, never()).createAccessToken(anyString(), any(Role.class));
		}
	}

	@Nested
	@DisplayName("토큰 재발급")
	class RefreshTokenTests {

		@Test
		@DisplayName("성공 - 유효한 리프레시 토큰으로 새 토큰들을 발급받는다")
		void refreshAccessToken_withValidToken_shouldReturnNewTokens() {
			// Given
			String refreshToken = "validRefreshToken";
			String email = "test@example.com";
			UserDetailsImpl userDetails = UserDetailsImpl.build(member);
			RefreshTokenService.RefreshTokenInfo refreshTokenInfo =
				new RefreshTokenService.RefreshTokenInfo(email, member.getId(), false);

			doNothing().when(jwtProvider).validateToken(refreshToken);
			when(refreshTokenService.findByToken(refreshToken)).thenReturn(Optional.of(refreshTokenInfo));
			when(jwtProvider.getEmailFromToken(refreshToken)).thenReturn(email);
			when(userDetailsService.getUserDetailsByEmail(email)).thenReturn(userDetails);
			doNothing().when(refreshTokenService).deleteByToken(refreshToken);

			when(jwtProvider.createAccessToken(anyString(), any(Role.class))).thenReturn("newAccessToken");
			when(refreshTokenService.createRefreshToken(anyString(), anyLong())).thenReturn("newRefreshToken");

			// When
			AuthResponse newTokens = authService.refreshAccessToken(refreshToken);

			// Then
			assertThat(newTokens.accessToken()).isEqualTo("newAccessToken");
			verify(refreshTokenService, times(1)).deleteByToken(refreshToken);
		}

		@Test
		@DisplayName("실패 - 만료된 리프레시 토큰으로 요청 시 예외가 발생한다")
		void refreshAccessToken_withExpiredToken_shouldThrowException() {
			// Given
			String expiredToken = "expiredRefreshToken";
			doThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED, "리프레시 토큰이 만료되었습니다."))
				.when(jwtProvider).validateToken(expiredToken);

			// When & Then
			assertThrows(BusinessException.class, () -> authService.refreshAccessToken(expiredToken));
		}

		@Test
		@DisplayName("실패 - 저장소에 존재하지 않는 리프레시 토큰으로 요청 시 예외가 발생한다")
		void refreshAccessToken_withNonExistentToken_shouldThrowException() {
			// Given
			String nonExistentToken = "nonExistentRefreshToken";
			doNothing().when(jwtProvider).validateToken(nonExistentToken);
			when(refreshTokenService.findByToken(nonExistentToken)).thenReturn(Optional.empty());

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> authService.refreshAccessToken(nonExistentToken));
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.TOKEN_INVALID);
		}
	}
}
