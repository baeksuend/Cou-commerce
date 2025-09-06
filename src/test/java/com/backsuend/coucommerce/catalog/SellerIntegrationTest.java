package com.backsuend.coucommerce.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("판매자 제품관리 통합테스트")
@WithMockUser(roles = "SELLER")
public class SellerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ProductRepository productRepository;

	Long member_id;
	Long product_id;
	Member member = null;
	String accessToken;
	List<Product> products = null;

	@BeforeEach
	void setUp() throws Exception {

		String password = "1234567890";
		String email = "hongheehdagu@naver.com";

		//가입, 로그인, 토큰 발급
		member = createMember(email, password, Role.SELLER);
		member_id = member.getId();
		accessToken = login(email, password);

		Product product1 = Product.builder().member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		Product product2 = Product.builder().member(member).name("딸기").detail("맛있는 딸기")
			.stock(50).price(20000).category(Category.FOOD).visible(true).build();
		Product product3 = Product.builder().member(member).name("포도").detail("맛있는 포도")
			.stock(60).price(30000).category(Category.FOOD).visible(true).build();
		List<Product> productList = List.of(product1, product2, product3);
		products = productRepository.saveAll(productList);
		product_id = product1.getId();

	}

	@AfterEach
	void tearDown() {
	}

	@Test
	@WithMockUser(roles = "SELLER")
	@DisplayName("판매자 제품 목록 조회 성공")
	void ProductList() throws Exception {

		//given

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/seller/products")
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
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.content[0].name").value("포도")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content[0].detail").value("맛있는 포도"))
			.andExpect(jsonPath("$.data.content[0].price").isNumber());

	}

	@Test
	@DisplayName("판매자 제품 상세내용 조회 성공")
	void ProductDetail() throws Exception {
		//given

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/seller/products/{id}", product_id)
				.header("Authorization", "Bearer " + accessToken)
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	@DisplayName("판매자 제품 상품등록 성공")
	void ProductCreate() throws Exception {

		//given
		ProductRequest productRequest = new ProductRequest("블루베리", "맛있는 블루베리", 100, 10000,
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
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.name").value("블루베리")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.detail").value("맛있는 블루베리"))
			.andExpect(jsonPath("$.data.price").isNumber());

	}

	@Test
	@DisplayName("판매자 제품 상품수정 성공")
	void ProductEdit() throws Exception {

		//given
		ProductRequest productRequest = new ProductRequest("유기농 블루베리", "맛있는 유기농 블루베리", 50, 20000,
			Category.FOOD, true);

		//when
		ResultActions resultActions = mockMvc.perform(
			put("/api/v1/seller/products/{id}", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productRequest))
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.name").value("유기농 블루베리")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.detail").value("맛있는 유기농 블루베리"))
			.andExpect(jsonPath("$.data.stock").value(50))
			.andExpect(jsonPath("$.data.price").isNumber());
	}

	@Test
	@DisplayName("판매자 제품 상품삭제 성공")
	void ProductDelete() throws Exception {

		//given

		//when
		ResultActions resultActions = mockMvc.perform(
			delete("/api/v1/seller/products/{id}", product_id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
		);

		//  then
		resultActions.andExpect(status().isNoContent());
	}

}
