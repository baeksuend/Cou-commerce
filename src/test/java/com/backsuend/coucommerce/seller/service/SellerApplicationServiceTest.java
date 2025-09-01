package com.backsuend.coucommerce.seller.service;

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
import com.backsuend.coucommerce.seller.dto.SellerApplicationRequest;
import com.backsuend.coucommerce.seller.entity.Seller;
import com.backsuend.coucommerce.seller.entity.SellerStatus;
import com.backsuend.coucommerce.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerApplicationService 단위 테스트")
class SellerApplicationServiceTest {

	@InjectMocks
	private SellerApplicationService sellerApplicationService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private SellerRepository sellerRepository;

	@Nested
	@DisplayName("판매자 전환 신청 (apply) 테스트")
	class ApplyForSellerTest {
		@Test
		@DisplayName("성공")
		void apply_success() {
			// given
			Long userId = 1L;
			SellerApplicationRequest request = new SellerApplicationRequest("테스트 상점", "123-45-67890");
			Member buyer = Member.builder().id(userId).role(Role.BUYER).build();

			when(memberRepository.findById(userId)).thenReturn(Optional.of(buyer));
			when(sellerRepository.existsByMember(buyer)).thenReturn(false);

			// when
			sellerApplicationService.apply(userId, request);

			// then
			ArgumentCaptor<Seller> sellerCaptor = ArgumentCaptor.forClass(Seller.class);
			verify(sellerRepository, times(1)).save(sellerCaptor.capture());
			Seller savedSeller = sellerCaptor.getValue();

			assertThat(savedSeller.getMember()).isEqualTo(buyer);
			assertThat(savedSeller.getStoreName()).isEqualTo(request.storeName());
			assertThat(savedSeller.getBusinessRegistrationNumber()).isEqualTo(request.businessRegistrationNumber());
			assertThat(savedSeller.getStatus()).isEqualTo(SellerStatus.APPLIED);
		}

		@Test
		@DisplayName("실패 - 이미 판매자/관리자인 경우")
		void apply_fail_whenNotBuyer() {
			// given
			Long userId = 1L;
			SellerApplicationRequest request = new SellerApplicationRequest("테스트 상점", "123-45-67890");
			Member seller = Member.builder().id(userId).role(Role.SELLER).build();

			when(memberRepository.findById(userId)).thenReturn(Optional.of(seller));

			// when & then
			assertThrows(BusinessException.class, () -> {
				sellerApplicationService.apply(userId, request);
			});
			verify(sellerRepository, never()).save(any(Seller.class));
		}

		@Test
		@DisplayName("실패 - 이미 신청한 경우")
		void apply_fail_whenAlreadyApplied() {
			// given
			Long userId = 1L;
			SellerApplicationRequest request = new SellerApplicationRequest("테스트 상점", "123-45-67890");
			Member buyer = Member.builder().id(userId).role(Role.BUYER).build();

			when(memberRepository.findById(userId)).thenReturn(Optional.of(buyer));
			when(sellerRepository.existsByMember(buyer)).thenReturn(true);

			// when & then
			assertThrows(BusinessException.class, () -> {
				sellerApplicationService.apply(userId, request);
			});
			verify(sellerRepository, never()).save(any(Seller.class));
		}
	}
}
