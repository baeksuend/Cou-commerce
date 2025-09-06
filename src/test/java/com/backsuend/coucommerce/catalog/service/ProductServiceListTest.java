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
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
public class ProductServiceListTest {

	@Mock
	ProductRepository productRepository;

	@Mock
	MemberRepository memberRepository;

	@Spy
	@InjectMocks
	ProductServiceImpl productService; // 실제 구현체 + mock 주입

	Pageable pageable;
	Page<Product> mockPage;
	Long member_id = 1L;
	Long product_id = 1L;
	Member member;

	@BeforeEach
	void setUp() {

		//회원 테이블 생성
		member = new Member(member_id, "hong@naver.com", "1111", "1112223333", "홍길동", Role.SELLER,
			MemberStatus.ACTIVE);

		//product 생성
		int page = 1;
		int pageSize = 10;
		pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

		Product product1 = Product.builder().id(product_id).member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		Product product2 = Product.builder().id(product_id).member(member).name("딸기").detail("맛있는 딸기")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		List<Product> productList = List.of(product1, product2);
		mockPage = new PageImpl<>(productList, pageable, productList.size());

	}

	@AfterEach
	void tearDown() {

	}

	@Test
	@DisplayName("삭제상품을 제외하고 ProductListType 형식별로 상품 목록을 조회한다.")
	void getProducts_success() {

		// Given
		Long member_id = 1L;
		String keyword = "";
		ProductListType listType = ProductListType.USER_LIST_ALL;
		ProductItemSearchRequest item = new ProductItemSearchRequest(1, 10, "name", "asc", "");

		//when
		doReturn(mockPage).when(productService)
			.getProductsListType(eq(listType), eq(member_id), eq(keyword), eq(null), any(Pageable.class));

		// then
		Page<ProductResponse> result = productService.getProducts(listType, item, member_id, null);

		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getName()).isEqualTo("바나나");
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");

	}

	@Test
	@DisplayName("일반 사용자가 삭제상품을 제외하고 상품 상세내용을 조회한다.")
	void getRead() {

		// Given
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
		ProductResponse result = productService.getRead(readType, product_id, member_id);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("바나나");
	}

	@Test
	@DisplayName("셀러나 관리자가 상품을 등록한다.")
	void getCreate() {

		// Given
		long member_id = 1L;

		ProductRequest dto = ProductRequest.builder()
			.name("파인애플").detail("맛있는 파인애플").stock(100).price(10000)
			.category(Category.FOOD).visible(true).build();
		Product product = dto.toEntity(member);

		when(memberRepository.findById(member_id)).thenReturn(Optional.of(member));
		when(productRepository.save(any(Product.class))).thenReturn(product);

		//when
		ProductResponse result = productService.getCreate(dto, member_id, null);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("파인애플");
	}

	@Test
	@DisplayName("셀러나 관리자가 상품을 수정한다.")
	void getEdit() {

		// Given
		long product_id = 1L;
		long member_id = 1L;

		ProductEditRequest dto = ProductEditRequest.builder().name("파인애플")
			.detail("맛있는 파인애플").stock(100).price(10000)
			.category(Category.FOOD).visible(true).build();

		Product product = dto.toEntity(member);

		Mockito.when(productRepository.findByDeletedAtIsNullAndIdAndMemberId(product_id, member_id))
			.thenReturn(Optional.of(product));

		when(memberRepository.findById(member_id)).thenReturn(Optional.of(member));
		when(productRepository.save(any(Product.class))).thenReturn(product);

		//when
		ProductResponse result = productService.getEdit(product_id, member_id, dto, null);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("파인애플");
	}

	@Test
	@DisplayName("셀러나 관리자가 상품을 삭제한다.")
	void getDelete() {

		// Given
		long product_id = 1L;
		long member_id = 1L;

		ProductEditRequest dto = ProductEditRequest.builder().name("파인애플")
			.detail("맛있는 파인애플").stock(100).price(10000)
			.category(Category.FOOD).visible(true).build();
		Product product = dto.toEntity(member);

		Mockito.when(productRepository.findByDeletedAtIsNullAndIdAndMemberId(product_id, member_id))
			.thenReturn(Optional.of(product));

		//when
		productService.getDelete(product_id, member_id);

		// then
		assertDoesNotThrow(() -> productService.checkExistsProduct(product_id, member_id));
	}

}
