package com.backsuend.coucommerce.review.service;

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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.service.ProductSummaryService;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;
import com.backsuend.coucommerce.review.entity.Review;
import com.backsuend.coucommerce.review.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName(".isNotNull(); 단위 테스트")
@WithMockUser(roles = "BUYER")
public class ReviewServiceTest {

	@Mock
	ReviewRepository reviewRepository;

	@Mock
	MemberRepository memberRepository;

	@Mock
	ProductSummaryService productSummaryService;

	@Spy
	@InjectMocks
	ReviewServiceImpl reviewService; // 실제 구현체 + mock 주입

	Pageable pageable;
	Page<Review> mockPage;
	Page<Product> mockProduct;
	Long memberId = 1L;
	Long productId = 1L;
	Member member = null;
	Product product = null;

	@BeforeEach
	void setUp() {

		//member 생성
		member = Member.builder().id(memberId).email("hongheeheeedagu@naver.com").password("12345678")
			.phone("010-222-3333").name("홍길동").role(Role.SELLER).status(MemberStatus.ACTIVE).build();

		//product 생성
		product = Product.builder().id(productId).member(member).name("바나나").detail("맛있는 바나나")
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

	}

	@AfterEach
	void tearDown() {

	}

	@Test
	@DisplayName("해당상품의 리뷰 목록을 조회한다.")
	void getReviews() {

		//given

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(productId) && p.getMember().getId().equals(memberId))
			.findFirst()
			.orElse(null);
		doReturn(mockCont).when(reviewService).validateProduct(eq(productId));
		doReturn(mockPage).when(reviewRepository).findByProductAndParentReviewIsNull(eq(mockCont), any(Pageable.class));

		//when
		Page<ReviewResponseDto> result = reviewService.getReviews(productId, 1, true);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent().getFirst().getContent()).isEqualTo("내용입니다.1");
	}

	@Test
	@DisplayName("로그인한 회원이 해당상품에 리뷰 생성한다.")
	void createReview() {

		// given

		Member mockMember = Member.builder()
			.id(memberId)
			.name("테스트유저")
			.build();

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(productId) && p.getMember().getId().equals(productId))
			.findFirst()
			.orElse(null);

		ReviewRequestDto dto = new ReviewRequestDto("내용입니다.1", 3, null);

		Review savedReview = Review.builder()
			.product(mockCont)
			.member(mockMember)
			.content(dto.getContent())
			.parentReview(null)
			.build();

		// mock 정의
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
		doReturn(mockCont).when(reviewService).validateProduct(productId);
		when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

		doNothing().when(productSummaryService).setReviewCount(eq(dto.getAvgReviewScore()), eq(productId));

		// when
		ReviewResponseDto result = reviewService.createReview(productId, dto, memberId);

		// then
		assertNotNull(result);
		assertEquals(dto.getContent(), result.getContent());
		assertEquals(mockMember.getId(), result.getMemberId());
		verify(reviewRepository).save(any(Review.class));
	}

	@Test
	@DisplayName("로그인한 회원이 해당상품에 자신의 리뷰 수정한다.")
	void updateReview() {

		// given
		Long reviewId = 1L;
		String content = "내용입니다.";

		Member mockMember = Member.builder()
			.id(memberId)
			.name("테스트유저")
			.build();

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(productId) && p.getMember().getId().equals(memberId))
			.findFirst()
			.orElse(null);

		ReviewRequestDto dto = new ReviewRequestDto("내용입니다.1", 3, null);

		Review review = Review.builder()
			.id(reviewId)
			.product(mockCont)
			.member(mockMember)
			.content(content)
			.parentReview(null)
			.build();

		// mock 정의
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
		doReturn(mockCont).when(reviewService).validateProduct(productId);
		doReturn(review).when(reviewService).validateReviewOwnership(productId, reviewId, mockMember);

		// when
		ReviewResponseDto result = reviewService.updateReview(productId, reviewId, dto, memberId);

		// then
		assertNotNull(result);
		assertEquals(dto.getContent(), result.getContent());
		assertEquals(mockMember.getId(), result.getMemberId());
	}

	@Test
	@DisplayName("로그인한 회원이 해당상품에 자신의 리뷰를 삭제한다.")
	void deleteReview() {

		// given

		Long reviewId = 1L;

		Product mockCont = mockProduct.getContent().stream()
			.filter(p -> p.getId().equals(productId) && p.getMember().getId().equals(memberId))
			.findFirst()
			.orElse(null);

		ReviewRequestDto dto = new ReviewRequestDto("내용입니다.1", 3, null);

		Review review = Review.builder()
			.id(reviewId)
			.product(mockCont)
			.member(member)
			.content(dto.getContent())
			.parentReview(null)
			.build();

		// mock 정의
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		doReturn(review).when(reviewService).validateReviewOwnership(productId, reviewId, member);

		// when & then
		assertDoesNotThrow(() -> reviewService.deleteReview(productId, reviewId, memberId));

	}
}
