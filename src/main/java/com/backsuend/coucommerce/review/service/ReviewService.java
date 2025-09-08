package com.backsuend.coucommerce.review.service;

import org.springframework.data.domain.Page;

import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;

public interface ReviewService {

	Page<ReviewResponseDto> getReviews(Long productId, int page, boolean isAsc);

	ReviewResponseDto readView(Long productId, Long reviewId, long memberId);

	ReviewResponseDto createReview(Long productId, ReviewRequestDto requestDto, long memberId);

	ReviewResponseDto updateReview(Long productId, Long reviewId, ReviewRequestDto requestDto, long memberId);

	void deleteReview(Long productId, Long reviewId, long memberId);

	void deleteChildReview(Long productId, Long reviewId, Long childReviewId, long memberId);

}
