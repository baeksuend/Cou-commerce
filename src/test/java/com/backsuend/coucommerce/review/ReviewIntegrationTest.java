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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.entity.Review;
import com.backsuend.coucommerce.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("seller 통합테스트")
@WithMockUser(roles = "BUYER")
public class ReviewIntegrationTest extends BaseIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	ReviewRepository reviewRepository;
	Long member_id;
	Long product_id;
	Member member = null;
	Product product = null;
	String accessToken;

	@BeforeEach
	void setUp() throws Exception {

		String password = "12345678";
		String email = "hongheeheeedagu@naver.com";

		//가입, 로그인, 토큰 발급
		member = createMember(email, password, Role.BUYER);
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

		System.out.println("accessToken111===>" + accessToken);

	}

	@AfterEach
	void tearDown() {
	}

	@Test
	@WithMockUser(roles = "SELLER")
	@DisplayName("구매자 리뷰 목록조회 성공")
	void ProductList() throws Exception {

		//given
		Review review1 = Review.builder().product(product).member(member).content("내용입니다.1")
			.parentReview(null).build();
		Review review2 = Review.builder().product(product).member(member).content("내용입니다.2")
			.parentReview(null).build();
		List<Review> reviewList = List.of(review1, review2);
		List<Review> reviews = reviewRepository.saveAll(reviewList);

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/{product_id}/reviews", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.param("page", "1")
				.param("sort", "")
				.param("sortDir", "")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.content[0].content").value("내용입니다.1"));

	}

	@Test
	@DisplayName("구매자 리뷰 상세내용 조회 성공")
	void ReviewDetail() throws Exception {
		//given

		Review review1 = Review.builder().product(product).member(member).content("내용입니다.1")
			.parentReview(null).build();
		Review review = reviewRepository.save(review1);

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/{product_id}/reviews/{review_id}",
				product_id, review.getId())
				.header("Authorization", "Bearer " + accessToken)
				.param("isAsc", "1")
				.param("page", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.id").value(review.getId())) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content").value("내용입니다.1"));

	}

	@Test
	@DisplayName("구매자 리뷰 등록 성공")
	void ReviewCreate() throws Exception {

		//given
		ReviewRequestDto dto = ReviewRequestDto.builder()
			.content("내용입니다.1").parentReviewId(null).build();

		//when
		ResultActions resultActions = mockMvc.perform(
			post("/api/v1/products/{product_id}/reviews", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))
		);

		//then
		resultActions.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.product_id").value(product_id)) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content").value("내용입니다.1"));

	}

	@Test
	@DisplayName("구매자 리뷰 수정 성공")
	void ReviewEdit() throws Exception {

		//given

		ReviewRequestDto requestDto = ReviewRequestDto.builder()
			.content("내용입니다.2").parentReviewId(null).build();

		Review review = Review.builder()
			.product(product).member(member).content(requestDto.getContent()).parentReview(null)
			.build();
		Review saved = reviewRepository.save(review);
		long review_id = saved.getId();

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

		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.product_id").value(product_id)) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content").value("내용입니다.2"));

	}

	@Test
	@DisplayName("구매자 리뷰 삭제 성공")
	void ReviewDelete() throws Exception {

		//given

		ReviewRequestDto requestDto = ReviewRequestDto.builder()
			.content("내용입니다.1").parentReviewId(null).build();

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
