package com.backsuend.coucommerce.member.service;

import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.member.dto.AddressChangeRequest;
import com.backsuend.coucommerce.member.dto.PasswordChangeRequest;
import com.backsuend.coucommerce.member.dto.UserProfileResponse;
import com.backsuend.coucommerce.member.repository.AddressRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private Address address;

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

        address = Address.builder()
            .id(1L)
            .member(member)
            .postalCode("12345")
            .roadName("Test Road")
            .detail("101")
            .build();
    }

    @Nested
    @DisplayName("프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("성공 - 사용자 ID로 프로필 정보를 성공적으로 조회한다")
        void getUserProfile_success() {
            // Given
            Long memberId = 1L;
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(addressRepository.findByMember(member)).thenReturn(Optional.of(address));

            // When
            UserProfileResponse response = memberService.getUserProfile(memberId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(member.getEmail());
            assertThat(response.name()).isEqualTo(member.getName());
            assertThat(response.postalCode()).isEqualTo(address.getPostalCode());

            verify(memberRepository).findById(memberId);
            verify(addressRepository).findByMember(member);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 ID로 조회 시 예외가 발생한다")
        void getUserProfile_fail_memberNotFound() {
            // Given
            Long memberId = 99L;
            when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                memberService.getUserProfile(memberId);
            });
        }
    }

    @Nested
    @DisplayName("주소 변경")
    class ChangeAddress {

        @Test
        @DisplayName("성공 - 주소 변경 요청 시 주소 정보가 업데이트된다")
        void changeAddress_success() {
            // Given
            Long memberId = 1L;
            AddressChangeRequest request = new AddressChangeRequest("54321", "New Road", "202");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(addressRepository.findByMember(member)).thenReturn(Optional.of(address));

            // When
            memberService.changeAddress(memberId, request);

            // Then
            verify(addressRepository).save(any(Address.class));
            assertThat(address.getPostalCode()).isEqualTo(request.postalCode());
            assertThat(address.getRoadName()).isEqualTo(request.roadName());
            assertThat(address.getDetail()).isEqualTo(request.detail());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 ID로 주소 변경 시 예외가 발생한다")
        void changeAddress_fail_memberNotFound() {
            // Given
            Long memberId = 99L;
            AddressChangeRequest request = new AddressChangeRequest("54321", "New Road", "202");
            when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                memberService.changeAddress(memberId, request);
            });
            verify(addressRepository, never()).save(any(Address.class));
        }

        @Test
        @DisplayName("실패 - 사용자의 주소 정보가 없을 때 주소 변경 시 예외가 발생한다")
        void changeAddress_fail_addressNotFound() {
            // Given
            Long memberId = 1L;
            AddressChangeRequest request = new AddressChangeRequest("54321", "New Road", "202");
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(addressRepository.findByMember(member)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                memberService.changeAddress(memberId, request);
            });
            verify(addressRepository, never()).save(any(Address.class));
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("성공 - 현재 비밀번호가 일치하면 새 비밀번호로 변경된다")
        void changePassword_success() {
            // Given
            Long memberId = 1L;
            PasswordChangeRequest request = new PasswordChangeRequest("oldPassword123!", "newPassword123!");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(passwordEncoder.matches(request.oldPassword(), member.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(request.newPassword())).thenReturn("newEncodedPassword");
            doNothing().when(refreshTokenService).deleteAllTokensForUser(memberId);

            // When
            memberService.changePassword(memberId, request);

            // Then
            verify(memberRepository).save(member);
            verify(refreshTokenService).deleteAllTokensForUser(memberId);
            assertThat(member.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 ID로 비밀번호 변경 시 예외가 발생한다")
        void changePassword_fail_memberNotFound() {
            // Given
            Long memberId = 99L;
            PasswordChangeRequest request = new PasswordChangeRequest("oldPassword123!", "newPassword123!");
            when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                memberService.changePassword(memberId, request);
            });
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(memberRepository, never()).save(any(Member.class));
            verify(refreshTokenService, never()).deleteAllTokensForUser(anyLong());
        }

        @Test
        @DisplayName("실패 - 현재 비밀번호가 일치하지 않으면 예외가 발생한다")
        void changePassword_fail_wrongOldPassword() {
            // Given
            Long memberId = 1L;
            PasswordChangeRequest request = new PasswordChangeRequest("wrongOldPassword", "newPassword123!");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(passwordEncoder.matches(request.oldPassword(), member.getPassword())).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                memberService.changePassword(memberId, request);
            });

            verify(memberRepository, never()).save(any(Member.class));
            verify(refreshTokenService, never()).deleteAllTokensForUser(anyLong());
        }
    }
}