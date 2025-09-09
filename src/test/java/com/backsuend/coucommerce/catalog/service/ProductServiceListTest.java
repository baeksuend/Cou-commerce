package com.backsuend.coucommerce.catalog.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductMainDisplay;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.enums.ProductSortType;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.repository.ProductThumbnailRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
public class ProductServiceListTest {

	@Mock
	ProductRepository productRepository;

	@Mock
	MemberRepository memberRepository;

	@Mock
	ProductSummaryService productSummaryService;
	@Mock
	ProductThumbnailServiceImpl productThumbnailService;
	@Spy
	@InjectMocks
	ProductServiceImpl productService; // 실제 구현체 + mock 주입
	Pageable pageable;
	Page<Product> mockPage;
	Member member;
	@Mock
	private ProductThumbnailRepository productThumbnailRepository;

	@BeforeEach
	void setUp() {

		Long memberId = 1L;
		Long productId = 1L;

		//회원 테이블 생성
		member = new Member(memberId, "hong@naver.com", "1111", "1112223333", "홍길동", Role.SELLER,
			MemberStatus.ACTIVE);

		//product 생성
		int page = 1;
		int pageSize = 10;
		pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

		Product product1 = Product.builder().id(productId).member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();
		Product product2 = Product.builder().id(productId).member(member).name("딸기").detail("맛있는 딸기")
			.stock(200).price(20000).category(Category.FOOD).visible(true).build();
		List<Product> productList = List.of(product1, product2);
		mockPage = new PageImpl<>(productList, pageable, productList.size());

	}

	@AfterEach
	void tearDown() {

	}

	@Test
	@DisplayName("메인에 삭제상품을 제외하고 진열된 상품중에 [인기상품]순으로 진열한다.")
	void getProductsMain_popular() {

		// Given
		ProductMainDisplay mainDisplay = ProductMainDisplay.MAIN_BEST;
		int page = 10;

		Pageable pageable = PageRequest.of(0, page, Sort.by(Sort.Order.desc("createdAt")));
		doReturn(mockPage).when(productRepository).searchMainBestProducts(eq(pageable));

		//when
		Page<ProductResponse> result = productService.getProductsMain(mainDisplay, page);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getName()).isEqualTo("바나나");
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");

	}

	@Test
	@DisplayName("메인에 삭제상품을 제외하고 진열된 상품중에 [리뷰많은순]으로 진열한다.")
	void getProductsMain_good_review() {

		// Given
		ProductMainDisplay mainDisplay = ProductMainDisplay.MAIN_GOOD_REVIEW;
		int page = 10;

		Pageable pageable = PageRequest.of(0, page, Sort.by(Sort.Order.desc("createdAt")));
		doReturn(mockPage).when(productRepository).searchMainManyReviewProducts(eq(pageable));

		// when
		Page<ProductResponse> result = productService.getProductsMain(mainDisplay, page);

		//then
		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getName()).isEqualTo("바나나");
		assertThat(result.getContent().get(1).getName()).isEqualTo("딸기");

	}

	@Test
	@DisplayName("일반 사용자가 삭제상품을 제외하고 상품목록을 조회한다.")
	void getProductsUser() {

		// Given
		Long memberId = 1L;
		String keyword = "";
		int page = 1;
		int blockPage = 10;
		ProductSortType sortType = ProductSortType.RECENT;
		Category cate = Category.FOOD;

		ProductItemSearchRequest req = new ProductItemSearchRequest(page, blockPage, sortType, keyword);
		Sort.Order order = Sort.Order.desc("createdAt");
		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), Sort.by(order));

		doReturn(mockPage).when(productService)
			.getProductsListTypeUser(eq(sortType), eq(memberId), eq(keyword), eq(cate), eq(pageable));

		// when
		Page<ProductResponse> result = productService.getProductsUser(req, memberId, cate);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getName()).isEqualTo("바나나");
		assertThat(result.getContent().getFirst().getPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("판매자가가 삭제상품을 제외하고 본인 상품목록을 조회한다.")
	void getProductsUser_seller() {

		// Given
		long memberId = 1L;
		String keyword = "";
		int page = 1;
		int blockPage = 10;
		ProductSortType sortType = ProductSortType.RECENT;
		Category cate = Category.FOOD;

		ProductItemSearchRequest req = new ProductItemSearchRequest(page, blockPage, sortType, keyword);
		Sort.Order order = Sort.Order.desc("createdAt");
		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), Sort.by(order));

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		//doReturn(member).when(memberRepository).findById(memberId);
		doReturn(mockPage).when(productService)
			.getProductsListTypeSeller(eq(sortType), eq(member), eq(keyword), eq(cate), eq(pageable));

		// when
		Page<ProductResponse> result = productService.getProductsSeller(req, memberId, cate);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getName()).isEqualTo("바나나");
		assertThat(result.getContent().getFirst().getPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("일반 사용자가 삭제상품을 제외하고 상품 상세내용을 조회한다.")
	void getRead() {

		// Given
		Long productId = 1L;
		Long memberId = 1L;
		ProductReadType readType = ProductReadType.USER_READ;

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		Product mockCont = mockPage.getContent().stream()
			.filter(p -> p.getId().equals(productId) && p.getMember().getId().equals(memberId))
			.findFirst()
			.orElse(null);

		doReturn(mockCont).when(productService)
			.getProductsReadType(eq(readType), eq(member), eq(memberId), eq(memberId));

		doNothing().when(productSummaryService).setViewCount(eq(productId));

		//when
		ProductResponse result = productService.getRead(readType, memberId, memberId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("바나나");
	}

	@Test
	@DisplayName("셀러나 관리자가 상품을 등록한다.")
	void getCreate() {

		// Given
		long memberId = 1L;

		ProductRequest dto = ProductRequest.builder()
			.name("파인애플").detail("맛있는 파인애플").stock(100).price(10000)
			.category(Category.FOOD).visible(true).build();
		Product product = dto.toEntity(member);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(productRepository.save(any(Product.class))).thenReturn(product);
		doNothing().when(productThumbnailService).uploadThumbnail(any(Product.class), isNull());

		//when
		ProductResponse result = productService.getCreate(dto, memberId, null);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("파인애플");
	}

	@Test
	@DisplayName("셀러나 관리자가 상품을 수정한다.")
	void getEdit() {
		// Given
		long productId = 1L;
		long memberId = 1L;

		ReflectionTestUtils.setField(member, "id", memberId);

		ProductEditRequest dto = ProductEditRequest.builder()
			.name("파인애플")
			.detail("맛있는 파인애플")
			.stock(100)
			.price(10000)
			.category(Category.FOOD)
			.visible(true)
			.build();

		Product product = dto.toEntity(member);
		ReflectionTestUtils.setField(product, "id", productId);

		when(productRepository.findByDeletedAtIsNullAndIdAndMember_Id(productId, memberId))
			.thenReturn(Optional.of(product));
		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(productRepository.save(any(Product.class))).thenReturn(product);

		when(productThumbnailRepository.existsByProduct_Id(eq(productId))).thenReturn(true);
		doNothing().when(productThumbnailService).uploadThumbnail(any(Product.class), eq(Collections.emptyList()));

		// When
		ProductResponse result = productService.getEdit(productId, memberId, dto, Collections.emptyList());

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("파인애플");
		assertThat(result.getPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("셀러나 관리자가 상품을 삭제한다.")
	void getDelete() {

		// Given
		long productId = 1L;
		long memberId = 1L;

		ProductEditRequest dto = ProductEditRequest.builder().id(productId).name("파인애플")
			.detail("맛있는 파인애플").stock(100).price(10000)
			.category(Category.FOOD).visible(true).build();
		Product product = dto.toEntity(member);

		Mockito.when(productRepository.findByDeletedAtIsNullAndIdAndMember_Id(productId, memberId))
			.thenReturn(Optional.of(product));

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		Mockito.when(productRepository.findSellerRead(member, productId))
			.thenReturn(Optional.of(product));

		//when
		productService.getDelete(productId, memberId);

		// then
		assertDoesNotThrow(() -> productService.checkExistsProduct(productId, memberId));
	}

}
