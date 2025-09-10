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
	//Long memberId = 1L;
	Member member;

	@BeforeEach
	void setUp() {

		//회원 테이블 생성
		member = Member.builder()
			.id(1L)
			.email("hong@naver.com")
			.password("1111")
			.phone("1112223333")
			.name("홍길동")
			.role(Role.SELLER)
			.status(MemberStatus.ACTIVE)
			.build();

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
		long productId = 1L;
		long memberId = 1L;

		Product product = Product.builder().id(1L).member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();

		// mock 리포지토리가 특정 조건에서 가짜 데이터를 리턴하도록 설정
		Mockito.when(productRepository.findByDeletedAtIsNullAndIdAndMember_Id(productId, memberId))
			.thenReturn(Optional.of(product));

		// when & then
		assertDoesNotThrow(() -> productService.checkExistsProduct(productId, memberId));

	}

	@Test
	@DisplayName("비회원 사용자의 삭제안되고 진열된 상품을 카테고리별(USER_LIST_CATEGORY)별로 상품목록 가져온다.")
	void getProductsListTypeUser() {

		//given
		long memberId = 1L;
		String keyword = "";
		String sort = "RECENT";
		String cate = "BOOKS";

		when(
			productService.getProductsListTypeUser(ProductSortType.valueOf(sort), memberId,
				keyword, Category.valueOf(cate), pageable))
			.thenReturn(mockPage);

		//when
		Page<Product> result = productService.getProductsListTypeUser(ProductSortType.valueOf(sort), memberId,
			keyword, Category.valueOf(cate), pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");
		assertThat(result.getContent().getFirst().getPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("셀러회원이 삭제안되고 진열된 상품(SELLER_LIST_ALL)을 가져온다.")
	void getProductsListTypeSeller() {

		//given
		String keyword = "";
		String sort = "RECENT";
		String cate = "BOOKS";

		when(productService.getProductsListTypeSeller(ProductSortType.valueOf(sort), member,
			keyword, Category.valueOf(cate), pageable))
			.thenReturn(mockPage);

		//when
		Page<Product> result = productService.getProductsListTypeSeller(ProductSortType.valueOf(sort), member,
			keyword, Category.valueOf(cate), pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");
	}

	@Test
	@DisplayName("셀러회원이 삭제안되고 진열된 상품(SELLER_LIST_ALL)을 가져온다.")
	void getProductsReadType() {

		//given
		Long productId = 1L;
		long memberId = 1L;
		ProductReadType readType = ProductReadType.USER_READ;

		Product mockCont = mockPage.getContent().stream()
			.filter(p -> p.getId().equals(productId) && p.getMember().getId().equals(memberId))
			.findFirst()
			.orElse(null);

		doReturn(mockCont).when(productService)
			.getProductsReadType(eq(readType), eq(member), eq(productId), eq(memberId));

		//when
		Product result = productService.getProductsReadType(readType, member, productId, memberId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("바나나");
		assertThat(result.getCategory()).isEqualTo(Category.FOOD);
	}

}
