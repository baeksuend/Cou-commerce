package com.backsuend.coucommerce.review.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.catalog.service.ProductSummaryService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.CustomValidationException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.exception.NotFoundException;
import com.backsuend.coucommerce.common.service.MdcLogging;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;
import com.backsuend.coucommerce.review.entity.Review;
import com.backsuend.coucommerce.review.repository.ReviewRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "상품상세내용 리뷰 기능", description = "상품상세내용에 리뷰 작성(추가), 수정, 조회, 삭제 기능")
@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	private final ProductRepository productRepository;
	private final ProductSummaryService productSummaryService;

	/**
	 *  상품 리뷰 목록 조회
	 **/
	@Override
	@Transactional(readOnly = true)
	public Page<ReviewResponseDto> getReviews(Long productId, int page, boolean isAsc) {

		try (var ignored = MdcLogging.withContexts(Map.of(
			"productId", String.valueOf(productId)
		))) {
			log.info("상품 리뷰 목록 요청");

			// 조회 시에는 유저 정보 검증 x

			// 상품상세내용 검증
			Product product = validateProduct(productId);

			// pageable 객체 생성
			Pageable pageable = validateAndCreatePageable(page, isAsc);

			// 페이징 처리
			Page<Review> reviewPage = reviewRepository.findByProductAndParentReviewIsNull(product, pageable);

			log.debug("상품 리뷰 목록 요청 완료  - reviewPage.getTotalElements={}", reviewPage.getTotalElements());

			// 대댓글 정보를 포함하여 DTO 변환
			return reviewPage.map(Review -> {

				log.debug("상품 리뷰 목록 대댓글 내용 요청 완료  - getChildReviews.count={}",
					(long)Review.getChildReviews().size());

				List<ReviewResponseDto> childReviews = Review.getChildReviews().stream()
					.map(child -> new ReviewResponseDto(child, Collections.emptyList())) // 대댓글의 대댓글은 고려하지 않음
					.collect(Collectors.toList());
				return new ReviewResponseDto(Review, childReviews);
			});
		}

	}

	/**
	 * 상품상세내용 리뷰 추가
	 **/
	@Override
	public ReviewResponseDto readView(Long productId, Long reviewId, long memberId) {

		try (var ignored = MdcLogging.withContexts(Map.of(
			"productId", String.valueOf(productId),
			"reviewId", String.valueOf(reviewId)
		))) {
			log.info("상품 리뷰 내용보기 요청");

			// 회원 정보가져오기
			findAuthenticatedUser(memberId);

			// 상품상세내용 검증
			validateProduct(productId);

			//저장된데이터 불러오기
			Review saved = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new CustomValidationException(ErrorCode.NOT_FOUND, "등록된 리뷰 내용이 없습니다."));

			return new ReviewResponseDto(saved);
		}
	}

	/**
	 * 상품상세내용 리뷰 추가
	 **/
	@Override
	@Transactional
	public ReviewResponseDto createReview(Long productId,
		ReviewRequestDto requestDto, long memberId) {

		try (var ignored = MdcLogging.withContexts(Map.of(
			"productId", String.valueOf(productId)
		))) {
			log.info("상품 리뷰 등록 요청");

			// 회원 정보가져오기
			Member member = findAuthenticatedUser(memberId);
			log.debug("회원 정보 조회 완료: {}", member.getId());

			// 상품상세내용 검증
			Product product = validateProduct(productId);
			log.debug("상품 검증 완료: {}", product.getId());

			// 부모 리뷰 여부 확인
			Review parentReview;
			if (requestDto.getParentReviewId() != null) {
				parentReview = reviewRepository.findById(requestDto.getParentReviewId())
					.orElseThrow(() -> {
						log.error("부모 리뷰 없음: {}", requestDto.getParentReviewId());
						return new CustomValidationException(ErrorCode.NOT_FOUND);
					});
			} else {
				parentReview = null;
			}

			// 리뷰 추가(생성)
			Review review = Review.builder()
				.product(product)
				.member(member)
				.content(requestDto.getContent())
				.avgReviewScore(requestDto.getAvgReviewScore())
				.parentReview(parentReview)
				.build();

			//저장된데이터 불러오기
			Review saved = reviewRepository.save(review);
			log.info("리뷰 저장 완료 - reviewId={}, content={}", saved.getId(), saved.getContent());

			//** (추가) 상품 리뷰 갯수 업데이트
			productSummaryService.setReviewCount(requestDto.getAvgReviewScore(), productId);
			log.debug("상품 리뷰 카운트 업데이트 완료 - productId={}", productId);

			return new ReviewResponseDto(saved);
		}
	}

	/**
	 * 상품상세내용 리뷰 수정
	 **/
	@Override
	@Transactional
	public ReviewResponseDto updateReview(Long productId, Long reviewId,
		ReviewRequestDto requestDto, long memberId) {

		try (var ignored = MdcLogging.withContexts(Map.of(
			"productId", String.valueOf(productId),
			"reviewId", String.valueOf(reviewId)
		))) {
			log.info("상품 리뷰 수정 요청");

			// 유저 정보 검증
			Member member = findAuthenticatedUser(memberId);

			// 상품상세내용 검증
			validateProduct(productId);

			// 상품상세내용, 리뷰 및 리뷰에 대한 유저 권한 검증
			Review review = validateReviewOwnership(productId, reviewId, member);

			//유저정보 수정하기
			review.updateReview(requestDto.getContent());
			review.updateAvgReviewScore(requestDto.getAvgReviewScore());
			log.debug("리뷰 수정 완료 - reviewId={}, newScore={}", reviewId, requestDto.getAvgReviewScore());

			//** (추가) 수정시 상품 리뷰 갯수 업데이트
			productSummaryService.setReviewCountEdit(requestDto.getAvgReviewScore(), productId);

			return new ReviewResponseDto(review);
		}
	}

	/**
	 * 상품상세내용 리뷰 삭제 - 부모 리뷰
	 **/
	@Override
	@Transactional
	public void deleteReview(Long productId, Long reviewId, long memberId) {

		try (var ignored = MdcLogging.withContexts(Map.of(
			"productId", String.valueOf(productId),
			"reviewId", String.valueOf(reviewId)
		))) {
			log.info("상품 부모리뷰 삭제 요청");

			System.out.println("!111");

			// 유저 정보 검증
			Member member = findAuthenticatedUser(memberId);

			System.out.println("!222");

			// 상품상세내용, 리뷰 및 리뷰에 대한 유저 권한 검증
			Review review = validateReviewOwnership(productId, reviewId, member);

			System.out.println("!333");

			// 대리뷰이 있는 부모 리뷰인 경우, 내용만 "삭제된 리뷰입니다"로 변경
			if (!review.getChildReviews().isEmpty()) {
				System.out.println("!444");
				review.markAsDeleted();
				log.debug("부모 리뷰 삭제 처리(자식 존재) - reviewId={}", reviewId);

			} else {

				System.out.println("!555");
				//** (추가) 삭제시 상품 리뷰 갯수 업데이트
				productSummaryService.setReviewCountDelete(productId);

				System.out.println("!666");
				// 자식 리뷰이 없는 경우(단독 리뷰이거나 모든 자식 리뷰이 이미 삭제된 상태), 리뷰을 실제로 삭제
				reviewRepository.delete(review);
			}
		}
	}

	/**
	 * 상품상세내용 리뷰 삭제 - 자식 리뷰(대댓글)
	 **/
	@Override
	@Transactional
	public void deleteChildReview(Long productId, Long reviewId,
		Long childReviewId, long memberId) {

		try (var ignored = MdcLogging.withContexts(Map.of(
			"productId", String.valueOf(productId),
			"reviewId", String.valueOf(reviewId),
			"childReviewId", String.valueOf(childReviewId),
			"memberId", String.valueOf(memberId)
		))) {
			log.info("상품 자식리뷰 삭제 요청");

			// 유저 정보 검증
			Member member = findAuthenticatedUser(memberId);

			// 대댓글 조회
			log.debug("대댓글 조회 - childReviewId={}", childReviewId);
			Review childReview = reviewRepository.findById(childReviewId)
				.orElseThrow(() -> new CustomValidationException(ErrorCode.NOT_FOUND));

			// 대댓글의 작성자가 현재 사용자와 일치하는지 확인
			if (!childReview.getMember().getId().equals(member.getId())) {
				throw new CustomValidationException(ErrorCode.ACCESS_DENIED);
			}

			log.debug("Summary 삭제시 상품 리뷰 갯수 업데이트 - productId={}", productId);
			productSummaryService.setReviewCountDelete(productId);

			log.debug("자식 리뷰(대댓글) 삭제 - childReview={}", childReview);
			reviewRepository.delete(childReview);

			// 부모 리뷰이 삭제 상태일 경우 자식 리뷰이 모두 삭제되면 부모 리뷰을 DB에서 삭제
			Review parentReview = childReview.getParentReview();
			if (parentReview != null && parentReview.isDeleted()) {
				/* 모든 자식 리뷰이 삭제되었는지 확인 */
				long remainingChildren = parentReview.getChildReviews().stream()
					.filter(c -> !c.getId().equals(childReviewId) && !c.isDeleted())
					.count();

				if (remainingChildren == 0) {
					log.debug("부모 리뷰 삭제 - parentReview={}", parentReview);
					reviewRepository.delete(parentReview);
				}
			}

		}
	}

	/**
	 * 검증 메서드 필드
	 * 유저 정보 검증 메서드
	 **/
	public Member findAuthenticatedUser(long memberId) {
		log.debug("회원 검증 요청 - memberId={}", memberId);
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomValidationException(ErrorCode.UNAUTHORIZED, "등록된 회원이 아닙니다."));
	}

	/**
	 *  product 검증 메서드
	 **/
	public Product validateProduct(Long productId) {
		log.debug("상품 존재여부 검증 요청 - productId={}", productId);
		return productRepository.findById(productId)
			.orElseThrow(() -> new CustomValidationException(ErrorCode.VALIDATION_FAILED, "상품정보가 없습니다."));
	}

	/**
	 * 리뷰 및 리뷰에 대한 유저 권한 검증 메서드
	 **/
	public Review validateReviewOwnership(Long productId, Long reviewId, Member member) {

		log.info("리뷰 사용권한 검증 요청 - productId={}, reviewId={},memberId={}",
			productId, reviewId, member.getId());

		/* 상품 검증*/
		log.info("상품 검증 요청 검증 요청 - productId={}", productId);
		Product product = validateProduct(productId);

		/* 리뷰 검증*/
		log.info("리뷰 검증 요청 - productId={}, reviewId={}", productId, reviewId);
		Review review = reviewRepository.findByProductAndId(product, reviewId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "등록된 상품이나 리뷰가 없습니다."));

		/* 리뷰에 대한 유저 권한 검증*/
		log.info("리뷰 사용권한 요청 - productId={}, reviewId={}", productId, reviewId);
		if (!review.getMember().getId().equals(member.getId())) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}
		return review;
	}

	/**
	 *  입력 값 검증과 페이지 설정 메서드
	 **/
	public Pageable validateAndCreatePageable(int page, boolean isAsc) {

		log.debug("페이지, 정렬순서 설정 - page={}, isAsc={}", page, isAsc);
		if (page < 0) {
			throw new CustomValidationException(ErrorCode.INVALID_INPUT);
		}
		Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;

		int pageSize = 10;

		log.debug("Pageable 생성 - page={}, pageSize={}, isAsc={}", page, pageSize, isAsc);
		return PageRequest.of(page, pageSize, Sort.by(direction, "createdAt"));
	}

}
