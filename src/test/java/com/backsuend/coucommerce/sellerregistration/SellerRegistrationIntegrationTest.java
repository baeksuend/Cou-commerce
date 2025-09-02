package com.backsuend.coucommerce.sellerregistration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.sellerregistration.dto.CreateSellerRegistrationRequest;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;
import com.backsuend.coucommerce.sellerregistration.repository.SellerRegistrationRepository;

@DisplayName("SellerRegistration 기능 통합 테스트")
public class SellerRegistrationIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private SellerRegistrationRepository sellerRegistrationRepository;

	private String buyerToken;
	private Long buyerId;

	@BeforeEach
	void setUp() throws Exception {
		var buyer = createMember("buyer@example.com", "password123", Role.BUYER);
		buyerId = buyer.getId();
		buyerToken = login("buyer@example.com", "password123");
	}

	@Nested
	@DisplayName("판매자 전환 신청 (POST /api/v1/seller-registrations)")
	class ApplyForSeller {

		@Test
		@DisplayName("성공")
		void apply_success() throws Exception {
			// given
			CreateSellerRegistrationRequest request = new CreateSellerRegistrationRequest("테스트 상점", "123-45-67890");

			// when
			ResultActions result = mockMvc.perform(post("/api/v1/seller-registrations")
				.header("Authorization", "Bearer " + buyerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

			// then
			result.andExpect(status().isCreated());

			Optional<SellerRegistration> savedRegistration = sellerRegistrationRepository.findByMember(
				findMemberInNewTransaction(buyerId));
			assertThat(savedRegistration).isPresent();
			assertThat(savedRegistration.get().getStoreName()).isEqualTo("테스트 상점");
			assertThat(savedRegistration.get().getStatus()).isEqualTo(
				SellerRegistrationStatus.APPLIED);
		}

		@Test
		@DisplayName("실패 - 인증되지 않은 사용자")
		void apply_fail_unauthorized() throws Exception {
			// given
			CreateSellerRegistrationRequest request = new CreateSellerRegistrationRequest("테스트 상점", "123-45-67890");

			// when
			ResultActions result = mockMvc.perform(post("/api/v1/seller-registrations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

			// then
			result.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("실패 - 이미 신청한 경우")
		void apply_fail_duplicate() throws Exception {
			// given
			// 첫 번째 신청
			CreateSellerRegistrationRequest request = new CreateSellerRegistrationRequest("테스트 상점", "123-45-67890");
			mockMvc.perform(post("/api/v1/seller-registrations")
				.header("Authorization", "Bearer " + buyerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

			// when
			// 두 번째 신청
			ResultActions result = mockMvc.perform(post("/api/v1/seller-registrations")
				.header("Authorization", "Bearer " + buyerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

			// then
			result.andExpect(status().isConflict())
				.andExpect(jsonPath("$.data.message").value("이미 판매자 신청을 했거나 판매자입니다."));
		}
	}
}
