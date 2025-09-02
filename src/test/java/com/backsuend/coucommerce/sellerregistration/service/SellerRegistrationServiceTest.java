package com.backsuend.coucommerce.sellerregistration.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.sellerregistration.dto.CreateSellerRegistrationRequest;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;
import com.backsuend.coucommerce.sellerregistration.repository.SellerRegistrationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerRegistrationService 단위 테스트")
class SellerRegistrationServiceTest {

	@InjectMocks
	private SellerRegistrationService sellerRegistrationService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private SellerRegistrationRepository sellerRegistrationRepository;

	@Nested
	@DisplayName("판매자 전환 신청 (apply) 테스트")
	class ApplyForSellerTest {
		@Test
		@DisplayName("성공")
		void apply_success() {
			// given
			Long userId = 1L;
			CreateSellerRegistrationRequest request = new CreateSellerRegistrationRequest("테스트 상점", "123-45-67890");
			Member buyer = Member.builder().id(userId).role(Role.BUYER).build();

			when(memberRepository.findById(userId)).thenReturn(Optional.of(buyer));
			when(sellerRegistrationRepository.existsByMember(buyer)).thenReturn(false);

			// when
			sellerRegistrationService.apply(userId, request);

			// then
			ArgumentCaptor<SellerRegistration> sellerCaptor = ArgumentCaptor.forClass(SellerRegistration.class);
			verify(sellerRegistrationRepository, times(1)).save(sellerCaptor.capture());
			SellerRegistration savedSeller = sellerCaptor.getValue();

			assertThat(savedSeller.getMember()).isEqualTo(buyer);
			assertThat(savedSeller.getStoreName()).isEqualTo(request.storeName());
			assertThat(savedSeller.getBusinessRegistrationNumber()).isEqualTo(request.businessRegistrationNumber());
			assertThat(savedSeller.getStatus()).isEqualTo(SellerRegistrationStatus.APPLIED);
		}

		@Test
		@DisplayName("실패 - 이미 판매자/관리자인 경우")
		void apply_fail_whenNotBuyer() {
			// given
			Long userId = 1L;
			CreateSellerRegistrationRequest request = new CreateSellerRegistrationRequest("테스트 상점", "123-45-67890");
			Member seller = Member.builder().id(userId).role(Role.SELLER).build();

			when(memberRepository.findById(userId)).thenReturn(Optional.of(seller));

			// when & then
			assertThrows(BusinessException.class, () -> {
				sellerRegistrationService.apply(userId, request);
			});
			verify(sellerRegistrationRepository, never()).save(any(SellerRegistration.class));
		}

		@Test
		@DisplayName("실패 - 이미 신청한 경우")
		void apply_fail_whenAlreadyApplied() {
			// given
			Long userId = 1L;
			CreateSellerRegistrationRequest request = new CreateSellerRegistrationRequest("테스트 상점", "123-45-67890");
			Member buyer = Member.builder().id(userId).role(Role.BUYER).build();

			when(memberRepository.findById(userId)).thenReturn(Optional.of(buyer));
			when(sellerRegistrationRepository.existsByMember(buyer)).thenReturn(true);

			// when & then
			assertThrows(BusinessException.class, () -> {
				sellerRegistrationService.apply(userId, request);
			});
			verify(sellerRegistrationRepository, never()).save(any(SellerRegistration.class));
		}
	}
}
