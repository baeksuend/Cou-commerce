package com.backsuend.coucommerce.auth.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.backsuend.coucommerce.auth.entity.Role;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

	private JwtProvider jwtProvider;

	@BeforeEach
	void setUp() {
		jwtProvider = new JwtProvider();
		String testSecret = "VGVzdFNlY3JldEtleUZvckpXVEF1dGhlbnRpY2F0aW9uVGVzdFNlY3JldEtleUZvckpXVEF1dGhlbnRpY2F0aW9u"; // 64바이트 이상의 테스트 시크릿 키
		ReflectionTestUtils.setField(jwtProvider, "secret", testSecret);
		ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationTime", 3600000L); // 1시간
		ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpirationTime", 86400000L); // 24시간
		jwtProvider.init(); // @PostConstruct 메서드 수동 호출
	}

	@Nested
	@DisplayName("토큰 생성 및 검증")
	class TokenCreationAndValidation {

		@Test
		@DisplayName("성공 - 유효한 Access Token을 생성하고 검증한다")
		void createAndValidateAccessToken_success() {
			// Given
			String email = "test@example.com";
			Role role = Role.BUYER;

			// When
			String accessToken = jwtProvider.createAccessToken(email, role);

			// Then
			assertThat(accessToken).isNotNull();
			assertDoesNotThrow(() -> jwtProvider.validateToken(accessToken));
			assertThat(jwtProvider.getEmailFromToken(accessToken)).isEqualTo(email);
		}

		@Test
		@DisplayName("성공 - 유효한 Refresh Token을 생성하고 검증한다")
		void createAndValidateRefreshToken_success() {
			// Given
			String email = "test@example.com";

			// When
			String refreshToken = jwtProvider.createRefreshToken(email);

			// Then
			assertThat(refreshToken).isNotNull();
			assertDoesNotThrow(() -> jwtProvider.validateToken(refreshToken));
			assertThat(jwtProvider.getEmailFromToken(refreshToken)).isEqualTo(email);
		}

		@Test
		@DisplayName("실패 - 만료된 토큰을 검증하면 ExpiredJwtException 예외가 발생한다")
		void validateToken_shouldThrowException_forExpiredToken() throws InterruptedException {
			// Given
			// 만료 시간을 0으로 설정하여 즉시 만료되는 토큰 생성
			ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationTime", 0L);
			String expiredToken = jwtProvider.createAccessToken("test@example.com", Role.BUYER);

			// 토큰 생성 시간과 검증 시간 사이에 약간의 딜레이를 주어 확실히 만료되도록 함
			Thread.sleep(1);

			// When & Then
			assertThrows(ExpiredJwtException.class, () -> jwtProvider.validateToken(expiredToken));
		}

		@Test
		@DisplayName("실패 - 잘못된 서명을 가진 토큰을 검증하면 SignatureException 예외가 발생한다")
		void validateToken_shouldThrowException_forInvalidSignature() {
			// Given
			String email = "test@example.com";
			Role role = Role.BUYER;
			String token = jwtProvider.createAccessToken(email, role);

			// 다른 시크릿 키를 가진 새로운 JwtProvider 인스턴스 생성
			JwtProvider anotherJwtProvider = new JwtProvider();
			String anotherSecret = "VGhpc0lzQW5vdGhlckRpZmZlcmVudEJ1dEVxdWFsbHlMb25nQW5kU2VjdXJlU2VjcmV0S2V5Rm9yVGVzdGluZ1RoZVNpZ25hdHVyZVZhbGlkYXRpb24xMjM=";
			ReflectionTestUtils.setField(anotherJwtProvider, "secret", anotherSecret);
			anotherJwtProvider.init();

			// When & Then
			assertThrows(SignatureException.class, () -> anotherJwtProvider.validateToken(token));
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 형식의 토큰을 검증하면 JwtException 예외가 발생한다")
		void validateToken_shouldThrowException_forMalformedToken() {
			// Given
			String malformedToken = "this.is.not.a.jwt";

			// When & Then
			assertThrows(JwtException.class, () -> jwtProvider.validateToken(malformedToken));
		}
	}
}
