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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Product 통합테스트")
public class ProductIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	MemberRepository memberRepository;

	Long member_id;
	Long product_id;
	Member member = null;
	List<Product> products = null;

	@BeforeEach
	void setUp() {

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
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.content[0].name").value("포도")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content[0].detail").value("맛있는 포도"))
			.andExpect(jsonPath("$.data.content[0].price").isNumber());

	}

	@Test
	@DisplayName("제품 카테고리별 목록 조회 성공")
	void ProductCategoryList() throws Exception {

		//given

		//when
		String category = "FOOD";
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/products/category/{category}", category)
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
			.andExpect(jsonPath("$.data.content[1].name").value("딸기")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.content[1].detail").value("맛있는 딸기"))
			.andExpect(jsonPath("$.data.content[1].price").isNumber());
	}

	@Test
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
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.name").value("바나나")) // 최신순 정렬로 보임
			.andExpect(jsonPath("$.data.detail").value("맛있는 바나나"))
			.andExpect(jsonPath("$.data.price").isNumber());

	}

}
