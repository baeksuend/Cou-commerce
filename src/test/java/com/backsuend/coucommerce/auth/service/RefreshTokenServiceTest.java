package com.backsuend.coucommerce.auth.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.backsuend.coucommerce.auth.jwt.JwtProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService 단위 테스트")
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        // Mockito가 redisTemplate.opsForValue() 등을 호출했을 때
        // 실제 객체가 아닌 Mock 객체를 반환하도록 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Nested
    @DisplayName("토큰 삭제 (로그아웃)")
    class DeleteTokenTests {

        @Test
        @DisplayName("성공 - 주어진 리프레시 토큰을 저장소에서 삭제한다")
        void deleteByToken_success() throws JsonProcessingException {
            // Given
            String token = "validRefreshToken";
            String email = "test@example.com";
            Long userId = 1L;
            RefreshTokenService.RefreshTokenInfo info = new RefreshTokenService.RefreshTokenInfo(email, userId, false);
            String infoJson = "{\"email\":\"test@example.com\",\"userId\":1,\"used\":false}";

            String redisKey = "refreshToken:" + token;
            String userTokensKey = "user-tokens:" + userId;

            // readTokenInfo 내부 동작 Mocking
            when(valueOperations.get(redisKey)).thenReturn(infoJson);
            when(objectMapper.readValue(infoJson, RefreshTokenService.RefreshTokenInfo.class)).thenReturn(info);

            // deleteByToken 내부 동작 Mocking
            when(setOperations.remove(userTokensKey, token)).thenReturn(1L);
            when(redisTemplate.delete(redisKey)).thenReturn(true);

            // When
            refreshTokenService.deleteByToken(token);

            // Then
            verify(setOperations, times(1)).remove(userTokensKey, token);
            verify(redisTemplate, times(1)).delete(redisKey);
        }
    }
}
