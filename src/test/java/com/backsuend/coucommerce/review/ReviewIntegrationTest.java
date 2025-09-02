package com.backsuend.coucommerce.review;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.service.ProductServiceImpl;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.review.dto.ReviewEditRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.entity.Review;
import com.backsuend.coucommerce.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("seller 통합테스트")
@WithMockUser(roles = "SELLER")
public class ReviewIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ReviewRepository reviewRepository;
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

		String password = "12345678";
		member = Member.builder().email("hongheeheeedagu@naver.com")
			.password(passwordEncoder.encode(password))
			.phone("010-222-3333")
			.name("홍길동")
			.role(Role.BUYER)
			.status(MemberStatus.ACTIVE)
			.build();
		memberRepository.save(member);
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
		Product savedProduct = productRepository.save(product);
		product_id = savedProduct.getId();

		//로그인
		accessToken = login(member.getEmail(), password);

		System.out.println("accessToken111===>" + accessToken);

		//authentication 부여
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.BUYER.name()));
		testUserDetails = new UserDetailsImpl(member_id, member.getEmail(), member.getPassword(), authorities, true,
			true);
		authentication = new UsernamePasswordAuthenticationToken(testUserDetails, null, authorities);

	}

	//로그인
	protected String login(String email, String password) throws Exception {
		LoginRequest loginRequest = new LoginRequest(email, password);
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		String responseString = result.getResponse().getContentAsString();

		return objectMapper.readTree(responseString).get("data").get("accessToken").asText();
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	@Transactional
	@DisplayName("리뷰 목록 조회 성공")
	void ReviewList() throws Exception {

		//given
		Long userId = testUserDetails.getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/{product_id}/reviews", product_id)
				.param("isAsc", "1")
				.param("page", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("리뷰 등록 성공")
	void ReviewCreate() throws Exception {
		//given
		Long userId = testUserDetails.getId();

		ReviewRequestDto dto = ReviewRequestDto.builder().member_id(member_id)
			.product_id(product_id).content("내용입니다.1").parent_review_id(null).build();

		//when
		ResultActions resultActions = mockMvc.perform(
			post("/api/v1/products/{product_id}/reviews", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))
		);

		//then
		resultActions.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("리뷰 상세내용 조회 성공")
	void ReviewDetail() throws Exception {
		//given
		Long userId = testUserDetails.getId();

		Review review1 = Review.builder().member(member)
			.product(product).content("내용입니다.1").parentReview(null).build();
		Review review2 = reviewRepository.save(review1);
		long review_id = review2.getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/{product_id}/reviews/{review_id}",
				product_id, review_id)
				.header("Authorization", "Bearer " + accessToken)
				.param("isAsc", "1")
				.param("page", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("리뷰 수정 성공")
	void ReviewEdit() throws Exception {

		//given
		Long userId = testUserDetails.getId();

		ReviewEditRequestDto requestDto = ReviewEditRequestDto.builder()
			.product_id(product_id).content("내용입니다.1").parent_review_id(null).build();

		Review review = Review.builder()
			.product(product)
			.member(member)
			.content(requestDto.getContent())
			.parentReview(null)
			.build();
		Review saved = reviewRepository.save(review);
		long review_id = saved.getId();

		requestDto.setId(review_id);
		requestDto.setContent("내용입니다.2222");

		//when
		ResultActions resultActions = mockMvc.perform(
			put("/api/v1/products/{product_id}/reviews/{review_id}",
				product_id, review_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto))
		);

		//then
		resultActions.andExpect(status().isOk())
			.andDo(print())
			.andExpect(jsonPath("$.data").isNotEmpty());

	}

	@Test
	@Transactional
	@DisplayName("리뷰 삭제 성공")
	void ReviewDelete() throws Exception {

		//given
		Long userId = testUserDetails.getId();

		ReviewRequestDto requestDto = ReviewRequestDto.builder().member_id(userId)
			.product_id(product_id).content("내용입니다.1").parent_review_id(null).build();

		Review review = Review.builder()
			.product(product)
			.member(member)
			.content(requestDto.getContent())
			.parentReview(null)
			.build();
		Review saved = reviewRepository.save(review);
		long review_id = saved.getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			delete("/api/v1/products/{product_id}/reviews/{review_id}",
				product_id, review_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.param("member_id", String.valueOf(member_id))
		);

		//then
		resultActions.andExpect(status().isNoContent());
	}

}
