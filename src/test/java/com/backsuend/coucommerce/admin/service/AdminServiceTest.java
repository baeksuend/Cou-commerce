package com.backsuend.coucommerce.admin.service;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 단위 테스트")
class AdminServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AdminService adminService;

    private Member member;

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
}