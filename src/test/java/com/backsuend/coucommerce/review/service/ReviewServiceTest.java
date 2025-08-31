package com.backsuend.coucommerce.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.service.ProductServiceImpl;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.review.dto.ReviewDto;
import com.backsuend.coucommerce.review.dto.ReviewEditRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;
import com.backsuend.coucommerce.review.entity.Review;
import com.backsuend.coucommerce.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName(".isNotNull(); 단위 테스트")
@WithMockUser(roles = "BUYER")
public class ReviewServiceTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Mock
	ReviewRepository reviewRepository;

	@Mock
	MemberRepository memberRepository;

	@Mock
	ProductRepository productRepository;

	@InjectMocks
	ProductServiceImpl productService; // 실제 구현체 + mock 주입

	@Spy
	@InjectMocks
	ReviewServiceImpl reviewService; // 실제 구현체 + mock 주입

	Pageable pageable;
	Page<Review> mockPage;
	Page<ReviewResponseDto> mockPage2;
	Page<Product> mockProduct;
	Long member_id = 1L;
	Long product_id = 1L;
	Member member = null;
	Product product = null;

	private UserDetailsImpl testUserDetails;
	private Authentication authentication;

	@BeforeEach
	void setUp() {

		//member 생성
		member = Member.builder().id(member_id).email("hongheeheeedagu@naver.com").password("12345678")
			.phone("010-222-3333").name("홍길동").role(Role.SELLER).status(MemberStatus.ACTIVE).build();

		//product 생성
		product = Product.builder().id(product_id).member(member).name("바나나").detail("맛있는 바나나")
			.stock(100).price(10000).category(Category.FOOD).visible(true).build();

		//product 생성
		int page = 1;
		int pageSize = 10;
		pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

		Product p1 = Product.builder().id(1L).member(member).name("바나나").detail("맛있는 바나나").stock(100).price(10000)
			.category(Category.FOOD).visible(true).build();
		Product p2 = Product.builder().id(1L).member(member).name("딸기").detail("맛있는 딸기").stock(50).price(10000)
			.category(Category.FOOD).visible(true).build();
		List<Product> productList = List.of(p1, p2);
		mockProduct = new PageImpl<>(productList, pageable, productList.size());

		Review review1 = Review.builder().id(1L).product(product).member(member).content("내용입니다.1")
			.parentReview(null).build();
		Review review2 = Review.builder().id(1L).product(product).member(member).content("내용입니다.2")
			.parentReview(null).build();
		List<Review> list = List.of(review1, review2);
		mockPage = new PageImpl<>(list, pageable, list.size());

		// 테스트용 인증 객체 생성
		Long userId = 1L;
		String username = "test11testest@example.com";
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.BUYER.name()));
		testUserDetails = new UserDetailsImpl(userId, username, "password", authorities, true, true);
		authentication = new UsernamePasswordAuthenticationToken(testUserDetails, null, authorities);

	}

	@AfterEach
	void tearDown() {

	}

	@Test
	@DisplayName("해당상품의 리뷰 목록을 조회한다.")
	void getReviews() throws Exception {

		//given
		Long memberId = testUserDetails.getId();

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);
		doReturn(mockCont).when(reviewService).validateProduct(eq(product_id));
		doReturn(mockPage).when(reviewRepository).findByProductAndParentReviewIsNull(eq(mockCont), any(Pageable.class));

		//when
		Page<ReviewResponseDto> result = reviewService.getReviews(product_id, 1, true);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().get(0).getContent()).isEqualTo("내용입니다.1");
	}

	@Test
	@DisplayName("로그인한 회원이 해당상품에 리뷰 생성한다.")
	void createReview() {

		// given
		Long memberId = testUserDetails.getId();

		Member mockMember = Member.builder()
			.id(member_id)
			.name("테스트유저")
			.build();

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);

		ReviewRequestDto dto = new ReviewRequestDto(product_id, member_id, "내용입니다.1", null);

		Review savedReview = Review.builder()
			.product(mockCont)
			.member(mockMember)
			.content(dto.getContent())
			.parentReview(null)
			.build();

		// mock 정의
		when(memberRepository.findById(member_id)).thenReturn(Optional.of(mockMember));
		doReturn(mockCont).when(reviewService).validateProduct(product_id);
		when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

		// when
		ReviewResponseDto result = reviewService.createReview(product_id, dto, testUserDetails.getId());

		// then
		assertNotNull(result);
		assertEquals(dto.getContent(), result.getContent());
		assertEquals(mockMember.getId(), result.getMember_id());
		assertEquals(mockCont.getId(), result.getProduct_id());
		verify(reviewRepository).save(any(Review.class));
	}

	@Test
	@DisplayName("로그인한 회원이 해당상품에 자신의 리뷰 수정한다.")
	void updateReview() {

		// given
		Long memberId = testUserDetails.getId();
		Long review_id = 1L;

		Member mockMember = Member.builder()
			.id(member_id)
			.name("테스트유저")
			.build();

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);

		ReviewEditRequestDto dto = new ReviewEditRequestDto(1L, product_id, "내용입니다.1", null);

		Review review = Review.builder()
			.id(dto.getId())
			.product(mockCont)
			.member(mockMember)
			.content(dto.getContent())
			.parentReview(null)
			.build();

		// mock 정의
		when(memberRepository.findById(member_id)).thenReturn(Optional.of(mockMember));
		doReturn(mockCont).when(reviewService).validateProduct(product_id);
		doReturn(review).when(reviewService).validateReviewOwnership(product_id, review_id, mockMember);

		// when
		ReviewResponseDto result = reviewService.updateReview(product_id, review_id, dto, testUserDetails.getId());

		// then
		assertNotNull(result);
		assertEquals(dto.getId(), result.getId());
		assertEquals(dto.getContent(), result.getContent());
		assertEquals(mockMember.getId(), result.getMember_id());
		assertEquals(mockCont.getId(), result.getProduct_id());
	}

	@Test
	@DisplayName("로그인한 회원이 해당상품에 자신의 리뷰를 삭제한다.")
	void deleteReview() {

		// given
		Long memberId = testUserDetails.getId();

		Long review_id = 1L;

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(product_id) && p.getMember().getId().equals(member_id))
			.findFirst()
			.orElse(null);

		ReviewDto dto = new ReviewDto(review_id, product_id, member_id, "내용입니다.1",
			LocalDateTime.now(), null, null);

		Review review = Review.builder()
			.id(review_id)
			.product(mockCont)
			.member(member)
			.content(dto.getContent())
			.parentReview(null)
			.build();

		// mock 정의
		when(memberRepository.findById(member_id)).thenReturn(Optional.of(member));
		doReturn(review).when(reviewService).validateReviewOwnership(product_id, review_id, member);

		// when & then
		assertDoesNotThrow(() -> reviewService.deleteReview(product_id, review_id, testUserDetails.getId()));

	}
}
