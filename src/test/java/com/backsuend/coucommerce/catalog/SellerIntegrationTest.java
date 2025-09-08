package com.backsuend.coucommerce.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("판매자 제품관리 통합테스트")
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "SELLER")
public class SellerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ProductRepository productRepository;

	Long memberId;
	Long productId;
	Member member = null;
	String accessToken;
	List<Product> products = null;

	@BeforeEach
	void setUp() throws Exception {

		String password = "1234567890";
		String email = "hongheehdagu@naver.com";

		//가입, 로그인, 토큰 발급
		member = createMember(email, password, Role.SELLER);
		memberId = member.getId();
		accessToken = login(email, password);

		Product p1 = Product.builder().member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		Product p2 = Product.builder().member(member).name("딸기").detail("맛있는 딸기")
			.stock(50).price(20000).category(Category.FOOD).visible(true).build();
		Product p3 = Product.builder().member(member).name("포도").detail("맛있는 포도")
			.stock(60).price(30000).category(Category.FOOD).visible(true).build();
		List<Product> productList = List.of(p1, p2, p3);
		products = productRepository.saveAll(productList);
		//productId = product1.getId();

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
				.param("pageSize", "10")
				.param("sort", "RECENT")
				.param("keyword", "")
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.content[1].name").value("딸기")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content[1].detail").value("맛있는 딸기"))
			.andExpect(jsonPath("$.data.content[1].price").isNumber());

	}

	@Test
	@DisplayName("판매자 제품 상세내용 조회 성공")
	void ProductDetail() throws Exception {

		//given
		productId = products.get(2).getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/seller/products/{productId}", productId)
				.header("Authorization", "Bearer " + accessToken)
		);

		//then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isNotEmpty())
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.name").value("포도")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.detail").value("맛있는 포도"))
			.andExpect(jsonPath("$.data.price").isNumber());

	}

	@Test
	@DisplayName("판매자 제품 상품등록 성공")
	void ProductCreate() throws Exception {

		//given
		ProductRequest productRequest = ProductRequest.builder().name("블루베리").detail("맛있는 블루베리")
			.stock(100).price(10000).category(Category.FOOD).visible(true).images(null).build();

		MockMultipartFile imageFile = new MockMultipartFile(
			"images",
			"sample.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			"이미지 바이트".getBytes()
		);

		MockMultipartFile jsonPart = new MockMultipartFile(
			"productRequest",
			"",  // filename 없어도 됨
			MediaType.APPLICATION_JSON_VALUE,
			objectMapper.writeValueAsBytes(productRequest)
		);

		//when
		ResultActions resultActions = mockMvc.perform(
			multipart("/api/v1/seller/products")
				.file(imageFile)
				.file(jsonPart)
				.param("name", productRequest.getName())
				.param("detail", productRequest.getDetail())
				.param("stock", String.valueOf(productRequest.getStock()))
				.param("price", String.valueOf(productRequest.getPrice()))
				.param("category", String.valueOf(productRequest.getCategory()))

				.header("Authorization", "Bearer " + accessToken)
				.with(request -> {
					request.setMethod("POST");
					return request;
				}) // POST로 강제
		);

		//then
		resultActions.andExpect(status().isCreated())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.name").value("블루베리"))
			.andExpect(jsonPath("$.data.stock").value(100))
			.andExpect(jsonPath("$.data.price").value(10000));

	}

	@Test
	@DisplayName("판매자 제품 상품수정 성공")
	void ProductEdit() throws Exception {

		//given
		Product p4 = Product.builder().member(member).name("블루베리").detail("맛있는 블루베리")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		Product product = productRepository.save(p4);

		ProductEditRequest productEditRequest = ProductEditRequest.builder()
			.id(product.getId())
			.name("유기농 블루베리")
			.detail("맛있는 유기농 블루베리")
			.stock(50).price(10000).category(Category.FOOD)
			.visible(true).images(null).build();

		MockMultipartFile imageFile = new MockMultipartFile(
			"images",
			"sample.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			"이미지 바이트".getBytes()
		);

		MockMultipartFile jsonPart = new MockMultipartFile(
			"productEditRequest",
			"",  // filename 없어도 됨
			MediaType.APPLICATION_JSON_VALUE,
			objectMapper.writeValueAsBytes(productEditRequest)
		);

		//when
		ResultActions resultActions = mockMvc.perform(
			multipart("/api/v1/seller/products/{productId}", product.getId())
				.file(imageFile)
				.param("name", productEditRequest.getName())
				.param("detail", productEditRequest.getDetail())
				.param("stock", String.valueOf(productEditRequest.getStock()))
				.param("price", String.valueOf(productEditRequest.getPrice()))
				.param("category", String.valueOf(productEditRequest.getCategory()))
				.header("Authorization", "Bearer " + accessToken)
				.with(request -> {
					request.setMethod("PUT");
					return request;
				}) // POST로 강제
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
		productId = products.get(2).getId();

		//when
		ResultActions resultActions = mockMvc.perform(
			delete("/api/v1/seller/products/{id}", productId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
		);

		//  then
		resultActions.andExpect(status().isNoContent());
	}

}
