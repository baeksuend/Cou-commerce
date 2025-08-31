package com.backsuend.coucommerce.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.service.ProductServiceImpl;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Product 통합테스트")
public class ProductIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ProductServiceImpl productService; // 실제 구현체 + mock 주입

	Long member_id;
	Long product_id;
	Member member = null;
	Product product = null;

	String accessToken;

	private UserDetailsImpl testUserDetails;
	private Authentication authentication;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() throws Exception {

		String password = "12345678";
		member = Member.builder().email("hofngheeheeedagu@naver.com")
			.password(password)
			.phone("010-222-3333")
			.name("홍길동")
			.role(Role.SELLER)
			.status(MemberStatus.ACTIVE)
			.build();
		Member member2 = memberRepository.save(member);
		member_id = member2.getId();

		product = Product.builder()
			.member(member)
			.name("바나나")
			.detail("맛있는 바나나")
			.stock(100)
			.price(10000)
			.category(Category.FOOD)
			.visible(true)
			.build();
		Product product2 = productRepository.save(product);
		product_id = product2.getId();

	}

	@AfterEach
	void tearDown() {
	}

	@Test
	@Transactional
	@DisplayName("제품 목록 조회 성공")
	void ProductList() throws Exception {

		//given

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products")
				.param("page", "1")
				.param("sort", "")
				.param("sortDir", "")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("제품 카테고리별 목록 조회 성공")
	void ProductCategoryList() throws Exception {

		//given

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/category/{recate}", Category.FOOD)
				.param("page", "1")
				.param("sort", "")
				.param("sortDir", "")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("제품 상세내용 조회 성공")
	void ProductDetail() throws Exception {
		//given

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/{id}", product_id)
				.param("page", "1")
				.param("sort", "")
				.param("sortDir", "")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

}
