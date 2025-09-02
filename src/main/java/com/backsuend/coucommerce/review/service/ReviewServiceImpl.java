package com.backsuend.coucommerce.review.service;

import java.util.Collections;
import java.util.List;
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
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.review.dto.ReviewEditRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;
import com.backsuend.coucommerce.review.entity.Review;
import com.backsuend.coucommerce.review.repository.ReviewRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "상품상세내용 리뷰 기능", description = "상품상세내용에 리뷰 작성(추가), 수정, 조회, 삭제 기능")
@Slf4j(topic = "리뷰 생성, 수정, 삭제")
@Service
@Transactional
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	private final ProductRepository productRepository;

	/**
	 *  상품상세내용 리뷰 목록 조회
	 **/
	@Override
	@Transactional(readOnly = true)
	public Page<ReviewResponseDto> getReviews(Long product_id, int page, boolean isAsc) {

		// 조회 시에는 유저 정보 검증 x

		// 상품상세내용 검증
		Product product = validateProduct(product_id);

		// pageable 객체 생성
		Pageable pageable = validateAndCreatePageable(page, isAsc);

		// 페이징 처리
		Page<Review> reviewPage = reviewRepository.findByProductAndParentReviewIsNull(product, pageable);

		// 대댓글 정보를 포함하여 DTO 변환
		return reviewPage.map(Review -> {
			List<ReviewResponseDto> childReviews = Review.getChildReviews().stream()
				.map(child -> new ReviewResponseDto(child, Collections.emptyList())) // 대댓글의 대댓글은 고려하지 않음
				.collect(Collectors.toList());
			return new ReviewResponseDto(Review, childReviews);
		});

	}

	/**
	 * 상품상세내용 리뷰 추가
	 **/
	@Override
	@Transactional
	public ReviewResponseDto createReview(Long product_id,
		ReviewRequestDto requestDto, long memberId) {

		// 회원 정보가져오기
		Member member = findAuthenticatedUser(memberId);

		// 상품상세내용 검증
		Product product = validateProduct(product_id);

		// 부모 리뷰 여부 확인
		Review parentReview;
		if (requestDto.getParent_review_id() != null) {
			parentReview = reviewRepository.findById(requestDto.getParent_review_id())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		} else {
			parentReview = null;
		}

		// 리뷰 추가(생성)
		Review review = Review.builder()
			.product(product)
			.member(member)
			.content(requestDto.getContent())
			.parentReview(parentReview)
			.build();

		//저장된데이터 불러오기
		Review saved = reviewRepository.save(review);

		return new ReviewResponseDto(saved);
	}

	/**
	 * 상품상세내용 리뷰 추가
	 **/
	@Override
	public ReviewResponseDto readView(Long product_id, Long review_id, long memberId) {

		// 회원 정보가져오기
		Member member = findAuthenticatedUser(memberId);

		// 상품상세내용 검증
		Product product = validateProduct(product_id);

		//저장된데이터 불러오기
		Review saved = reviewRepository.findById(review_id).orElse(null);

		return new ReviewResponseDto(saved);
	}

	/**
	 * 상품상세내용 리뷰 수정
	 **/
	@Override
	@Transactional
	public ReviewResponseDto updateReview(Long product_id, Long review_id,
		ReviewEditRequestDto requestDto, long memberId) {

		// 유저 정보 검증
		Member member = findAuthenticatedUser(memberId);

		// 상품상세내용 검증
		Product product = validateProduct(product_id);

		// 상품상세내용, 리뷰 및 리뷰에 대한 유저 권한 검증
		Review review = validateReviewOwnership(product_id, review_id, member);

		//유저정보 수정하기
		review.updateReview(requestDto.getContent());

		return new ReviewResponseDto(review);
	}

	/**
	 * 상품상세내용 리뷰 삭제 - 부모 리뷰
	 **/
	@Override
	@Transactional
	public void deleteReview(Long product_id, Long review_id, long memberId) {

		// 유저 정보 검증
		Member member = findAuthenticatedUser(memberId);

		// 상품상세내용, 리뷰 및 리뷰에 대한 유저 권한 검증
		Review review = validateReviewOwnership(product_id, review_id, member);

		// 대리뷰이 있는 부모 리뷰인 경우, 내용만 "삭제된 리뷰입니다"로 변경
		if (!review.getChildReviews().isEmpty()) {
			review.markAsDeleted();
		} else {
			// 자식 리뷰이 없는 경우(단독 리뷰이거나 모든 자식 리뷰이 이미 삭제된 상태), 리뷰을 실제로 삭제
			reviewRepository.delete(review);
		}

	}

	/**
	 * 상품상세내용 리뷰 삭제 - 자식 리뷰(대댓글)
	 **/
	@Override
	@Transactional
	public void deleteChildReview(Long product_id, Long review_id,
		Long childReviewId, long memberId) {

		// 유저 정보 검증
		Member member = findAuthenticatedUser(memberId);

		// 대댓글 조회
		Review childReview = reviewRepository.findById(childReviewId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		// 대댓글의 작성자가 현재 사용자와 일치하는지 확인
		if (!childReview.getMember().getId().equals(member.getId())) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}

		// 자식 리뷰(대댓글) 삭제
		reviewRepository.delete(childReview);

		// 부모 리뷰이 삭제 상태일 경우 자식 리뷰이 모두 삭제되면 부모 리뷰을 DB에서 삭제
		Review parentReview = childReview.getParentReview();
		if (parentReview != null && parentReview.isDeleted()) {
			/* 모든 자식 리뷰이 삭제되었는지 확인 */
			long remainingChildren = parentReview.getChildReviews().stream()
				.filter(c -> !c.getId().equals(childReviewId) && !c.isDeleted())
				.count();

			if (remainingChildren == 0) {
				reviewRepository.delete(parentReview);
			}
		}
	}

	/**
	 * 검증 메서드 필드
	 * 유저 정보 검증 메서드
	 **/
	public Member findAuthenticatedUser(long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "접근권한이 없습니다."));
	}

	/**
	 *  product 검증 메서드
	 **/
	public Product validateProduct(Long product_id) {
		return productRepository.findById(product_id)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "상품정보가 없습니다."));
	}

	/**
	 * 리뷰 및 리뷰에 대한 유저 권한 검증 메서드
	 **/
	public Review validateReviewOwnership(Long product_id, Long review_id, Member member) {
		/* 게시글 검증*/
		Product product = validateProduct(product_id);

		/* 리뷰 검증*/
		Review review = reviewRepository.findByProductAndId(product, review_id)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		/* 리뷰에 대한 유저 권한 검증*/
		if (!review.getMember().getId().equals(member.getId())) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}
		return review;
	}

	/**
	 *  입력 값 검증과 페이지 설정 메서드
	 **/
	public Pageable validateAndCreatePageable(int page, boolean isAsc) {
		if (page < 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
		return PageRequest.of(page, 10, Sort.by(direction, "createdAt"));
	}

}
