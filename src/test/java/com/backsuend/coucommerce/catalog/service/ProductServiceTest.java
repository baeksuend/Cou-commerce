package com.backsuend.coucommerce.catalog.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.enums.ProductSortType;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
public class ProductServiceTest {

	@Mock
	ProductRepository productRepository;

	@Spy
	@InjectMocks
	ProductServiceImpl productService; // 실제 구현체 + mock 주입

	Pageable pageable;
	Page<Product> mockPage;
	Long member_id = 1L;

	@BeforeEach
	void setUp() {

		//회원 테이블 생성
		Member member = new Member(member_id, "hong@naver.com", "1111", "1112223333", "홍길동", Role.SELLER,
			MemberStatus.ACTIVE);

		//product 생성
		int page = 1;
		int pageSize = 10;
		pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

		Product product1 = Product.builder().id(1L).member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		Product product2 = Product.builder().id(2L).member(member).name("딸기").detail("맛있는 딸기")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();

		List<Product> productList = List.of(product1, product2);
		mockPage = new PageImpl<>(productList, pageable, productList.size());

	}

	@AfterEach
	void tearDown() {

	}

	@Test
	@DisplayName("정렬항목 널(null)이고 정렬방식 널(null) 일 경우 등록일(createdAt) 내림차순(desc)으로 정렬한다.")
	void checkBuildSortOrder_defaultValues_desc() {
		Sort.Order order = productService.checkBuildSortOrder(ProductSortType.RECENT);
		assertThat(order).isNotNull();
		assertThat(order.getProperty()).isEqualTo("createdAt");
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("정렬항목 이름(name)이고 정렬방식 널(null) 일 경우 이름(name) 내림차순(desc)으로 정렬한다.")
	void checkBuildSortOrder_nameValues1_asc() {
		Sort.Order order = productService.checkBuildSortOrder(ProductSortType.RECENT);
		assertThat(order).isNotNull();
		assertThat(order.getProperty()).isEqualTo("name");
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("정렬항목 널(null)이고 정렬방식 오름차순(asc) 일 경우 등록일(createdAt) 순서대로(ASC)으로 정렬한다.")
	void checkBuildSortOrder_nameValues2_asc() {
		Sort.Order order = productService.checkBuildSortOrder(ProductSortType.RECENT);
		assertThat(order).isNotNull();
		assertThat(order.getProperty()).isEqualTo("createdAt");
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	@DisplayName("정렬항목 이름(name)이고 정렬방식 내림차순(desc) 일 경우 이름(name) 내림차순(DESC)으로 정렬한다.")
	void checkBuildSortOrder_nameValues3_asc() {
		Sort.Order order = productService.checkBuildSortOrder(ProductSortType.RECENT);
		assertThat(order).isNotNull();
		assertThat(order.getProperty()).isEqualTo("name");
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("카테고리를 분류(cate)값이 있을경우 문자형(String)으로 반환한다.")
	void checkCategoryNullCheck1() {
		String result = productService.checkCategoryNullCheck(Category.FOOD);
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("FOOD");
	}

	@Test
	@DisplayName("카테고리를 분류(cate)값이 없을경우 널(null)으로 반환한다.")
	void checkCategoryNullCheck2() {
		String result = productService.checkCategoryNullCheck(null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("셀러가 상품관리전에 본인상품여부를 체크한다.")
	void checkExistsMember() {

		//given
		long id = 1L;
		long member_id = 1L;

		Member member = new Member(1L, "hong@naver.com", "1111", "1112223333", "홍길동", Role.SELLER, MemberStatus.ACTIVE);

		Product product = Product.builder().id(1L).member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();

		// mock 리포지토리가 특정 조건에서 가짜 데이터를 리턴하도록 설정
		Mockito.when(productRepository.findByDeletedAtIsNullAndIdAndMemberId(id, member_id))
			.thenReturn(Optional.of(product));

		// when & then
		assertDoesNotThrow(() -> productService.checkExistsProduct(id, member_id));

	}

	@Test
	@DisplayName("삭제안되고 진열된 상품 전체목록(USER_LIST_ALL) 가져온다.")
	void getProductsListType1() {

		//given
		ProductListType listType = ProductListType.USER_LIST_ALL;
		long member_id = 1L;
		String keyword = "";

		when(productService.getProductsListType(listType, member_id, keyword, null, pageable))
			.thenReturn(mockPage);

		Page<Product> result = productService.getProductsListType(listType, member_id, keyword, null, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getName()).isEqualTo("바나나");
		assertThat(result.getContent().getFirst().getPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("삭제안되고 진열된 상품을 카테고리별(USER_LIST_CATEGORY)별로 상품목록 가져온다.")
	void getProductsListType2() {

		//given
		ProductListType listType = ProductListType.USER_LIST_CATEGORY;
		long member_id = 1L;
		String keyword = "";

		when(productService.getProductsListType(listType, member_id, keyword, null, pageable))
			.thenReturn(mockPage);

		Page<Product> result = productService.getProductsListType(listType, member_id, keyword, null, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");
		assertThat(result.getContent().getFirst().getPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("셀러회원이 삭제안되고 진열된 상품(SELLER_LIST_ALL)을 가져온다.")
	void getProductsListType3() {

		//given
		ProductListType listType = ProductListType.SELLER_LIST_ALL;
		long member_id = 1L;
		String keyword = "";

		when(productService.getProductsListType(listType, member_id, keyword, null, pageable))
			.thenReturn(mockPage);

		//when
		Page<Product> result = productService.getProductsListType(listType, member_id, keyword, null, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");
	}

	@Test
	@DisplayName("관리자에게 전체 상품목록(ADMIN_LIST_ALL)을 가져온다.")
	void getProductsListType4() {

		//given
		ProductListType listType = ProductListType.ADMIN_LIST_ALL;
		long member_id = 1L;
		String keyword = "";

		when(productService.getProductsListType(listType, member_id, keyword, null, pageable))
			.thenReturn(mockPage);

		Page<Product> result = productService.getProductsListType(listType, member_id, keyword, null, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");
	}

	@Test
	@DisplayName("삭제안되고 진열된 상품의 상품상세내용 가져온다.")
	void getProductsReadType() {
		//given
		Long product_id = 1L;
		Long member_id = 1L;
		ProductReadType readType = ProductReadType.USER_READ;

		Product mockCont = mockPage.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);

		doReturn(mockCont).when(productService)
			.getProductsReadType(eq(readType), eq(product_id), eq(member_id));

		//when
		Product result = productService.getProductsReadType(readType, product_id, member_id);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("바나나");
	}

	@Test
	@DisplayName("셀러회원의 상품상세내용 가져오기 ")
	void getProductsReadType2() {

		//given
		ProductReadType readType = ProductReadType.SELLER_READ;
		long product_id = 1L;
		long member_id = 1L;

		Product mockCont = mockPage.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);

		doReturn(mockCont).when(productService)
			.getProductsReadType(eq(readType), eq(product_id), eq(member_id));

		//when
		Product result = productService.getProductsReadType(readType, product_id, member_id);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("바나나");
	}

	@Test
	@DisplayName("관리자의 상품상세내용 가져오기 ")
	void getProductsReadType3() {

		//given
		ProductReadType readType = ProductReadType.ADMIN_READ;
		long product_id = 1L;
		long member_id = 1L;

		Product mockCont = mockPage.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);

		doReturn(mockCont).when(productService)
			.getProductsReadType(eq(readType), eq(product_id), eq(member_id));

		//when
		Product result = productService.getProductsReadType(readType, product_id, member_id);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("바나나");
	}

}
