package com.backsuend.coucommerce.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.RefreshTokenRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.entity.Member;

@DisplayName("인증 통합 테스트")
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("회원가입")
    class RegisterTests {

        @Test
        @DisplayName("성공")
        void register_success() throws Exception {
            // Given
            SignupRequest signupRequest = new SignupRequest(
                "testuser@example.com",
                "password123!",
                "테스트유저",
                "010-1234-5678",
                "04538",
                "서울특별시 중구 세종대로 110",
                "101호"
            );

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

            // DB 검증
            Optional<Member> savedMemberOpt = memberRepository.findByEmail(signupRequest.email());
            assertThat(savedMemberOpt).isPresent();
            Member savedMember = savedMemberOpt.get();

            assertThat(savedMember.getEmail()).isEqualTo(signupRequest.email());
            assertThat(savedMember.getName()).isEqualTo(signupRequest.name());
            assertThat(savedMember.getPassword()).isNotEqualTo(signupRequest.password());
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 이메일")
        void register_fail_emailAlreadyExists() throws Exception {
            // Given
            createMember("existinguser@example.com", "password123!", com.backsuend.coucommerce.auth.entity.Role.BUYER);
            SignupRequest signupRequest = new SignupRequest(
                "existinguser@example.com", "newpassword123!", "새로운유저",
                "010-8765-4321", "12345", "서울특별시 강남구 테헤란로", "202호"
            );

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 입력값")
        void register_fail_invalidInput() throws Exception {
            // Given
            SignupRequest signupRequest = new SignupRequest(
                "", "password123!", "테스트유저", "010-1234-5678",
                "04538", "서울특별시 중구 세종대로 110", "101호"
            );

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("로그인")
    class LoginTests {

        @Test
        @DisplayName("성공 - API 응답과 Redis 저장 상태를 모두 검증")
        void login_success_and_verifyRedisState() throws Exception {
            // Given
            String email = "loginuser@example.com";
            String password = "password123!";
            createMember(email, password, com.backsuend.coucommerce.auth.entity.Role.BUYER);
            LoginRequest loginRequest = new LoginRequest(email, password);

            // When
            MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

            // Then - Redis 검증
            String responseBody = mvcResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(responseBody).get("data").get("refreshToken").asText();

            String redisKey = "refreshToken:" + refreshToken;
            String storedTokenInfo = redisTemplate.opsForValue().get(redisKey);

            assertThat(storedTokenInfo).isNotNull();
            assertThat(storedTokenInfo).contains(email);
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호")
        void login_fail_wrongPassword() throws Exception {
            // Given
            String email = "loginuser@example.com";
            String password = "password123!";
            createMember(email, password, com.backsuend.coucommerce.auth.entity.Role.BUYER);
            LoginRequest loginRequest = new LoginRequest(email, "wrongpassword");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class RefreshTokenTests {

        @Test
        @DisplayName("성공")
        void refresh_success() throws Exception {
            // Given
            String email = "refreshuser@example.com";
            String password = "password123!";
            createMember(email, password, com.backsuend.coucommerce.auth.entity.Role.BUYER);
            String refreshToken = getRefreshToken(email, password);
            RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(refreshToken);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 토큰")
        void refresh_fail_invalidToken() throws Exception {
            // Given
            String invalidToken = "this.is.an.invalid.token";
            RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(invalidToken);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class LogoutTests {

        @Test
        @DisplayName("성공 - 로그아웃 시 Redis에서 토큰이 삭제된다")
        void logout_success_and_verifyRedisState() throws Exception {
            // Given
            String email = "logoutuser@example.com";
            String password = "password123!";
            createMember(email, password, com.backsuend.coucommerce.auth.entity.Role.BUYER);
            String refreshToken = getRefreshToken(email, password);
            RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);

            // When - 로그아웃
            mockMvc.perform(post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

            // Then - Redis 검증
            String redisKey = "refreshToken:" + refreshToken;
            String storedTokenInfo = redisTemplate.opsForValue().get(redisKey);
            assertThat(storedTokenInfo).isNull();
        }
    }
}
