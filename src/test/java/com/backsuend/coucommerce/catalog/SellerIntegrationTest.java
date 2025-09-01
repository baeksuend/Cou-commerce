package com.backsuend.coucommerce.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.service.ProductServiceImpl;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

//@ActiveProfiles("test")
//@Transactional
//@SpringBootTest
//@AutoConfigureMockMvc
@DisplayName("Seller 통합테스트")
@WithMockUser(roles = "SELLER")
public class SellerIntegrationTest extends BaseIntegrationTest {

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
	@Autowired
	private PasswordEncoder passwordEncoder;
	private UserDetailsImpl testUserDetails;
	private Authentication authentication;

	@BeforeEach
	void setUp() throws Exception {

		String password = "1234567890";
		String email = "hongheehdagu@naver.com";
		String name = "홍길동";
		String phone = "010-222-3333";

		//가입, 로그인
		member = createMember(email, password, Role.SELLER);
		member_id = member.getId();

		accessToken = login(email, password);

		product = Product.builder()
			.member(member)
			.name("바나나")
			.detail("맛있는 바나나")
			.stock(100)
			.price(10000)
			.category(Category.FOOD)
			.visible(true)
			.build();
		Product savedProduct = productRepository.save(product);
		product_id = savedProduct.getId();

		//authentication 부여
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.SELLER.name()));
		testUserDetails = new UserDetailsImpl(member_id, member.getEmail(), member.getPassword(), authorities, true,
			true);
		authentication = new UsernamePasswordAuthenticationToken(testUserDetails, null, authorities);

	}


	@AfterEach
	void tearDown() {
	}

	@Test
	@WithMockUser(roles="SELLER")
	@Transactional
	@DisplayName("셀러 제품 목록 조회 성공")
	void ProductList() throws Exception {

		//given
		Long userId = testUserDetails.getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/seller/products")
				.header("Authorization", "Bearer " + accessToken)
				.param("page", "1")
				.param("sort", "")
				.param("sortd", "")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("셀러 상세내용 조회 성공")
	void ProductDetail() throws Exception {
		//given
		Long userId = testUserDetails.getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/seller/products/{id}", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.param("page", "1")
				.param("sort", "")
				.param("sortd", "")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("셀러 상품등록 성공")
	void ProductCreate() throws Exception {

		//given
		Long userId = testUserDetails.getId();
		ProductRequest productRequest = new ProductRequest(member_id,
			"블루베리", "맛있는 블루베리", 100, 10000,
			Category.FOOD, true);

		//when
		ResultActions resultActions = mockMvc.perform(
			post("/api/v1/seller/products")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productRequest))
		);

		//then
		resultActions.andExpect(status().isCreated())
			.andDo(print())
			.andExpect(jsonPath("$.data").isNotEmpty());

	}

	@Test
	@Transactional
	@DisplayName("셀러 상품수정 성공")
	void ProductEdit() throws Exception {

		//given
		Long userId = testUserDetails.getId();
		ProductEditRequest productEditRequest = new ProductEditRequest(product_id,
			"맛있는 블루베리", "맛있는 블루베리 내용",
			101, 10001,
			Category.FOOD, true);

		//when
		ResultActions resultActions = mockMvc.perform(
			put("/api/v1/seller/products/{id}", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productEditRequest))
		);

		//then
		resultActions.andExpect(status().isOk())
			.andDo(print())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("셀러 상품삭제 성공")
	void ProductDelete() throws Exception {

		//givenq
		Long userId = testUserDetails.getId();
		ProductEditRequest productEditRequest = new ProductEditRequest(product_id,
			"블루베리11", "맛있는 블루베리22",
			101, 10001,
			Category.FOOD, true);

		//when
		ResultActions resultActions = mockMvc.perform(
			delete("/api/v1/seller/products/{id}", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.param("member_id", String.valueOf(member_id))
				.content(objectMapper.writeValueAsString(productEditRequest))
		);

		//  then
		resultActions.andExpect(status().isNoContent());
	}

}
