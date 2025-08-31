package com.backsuend.coucommerce.review.service;

import org.springframework.data.domain.Page;

import com.backsuend.coucommerce.review.dto.ReviewEditRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;

public interface ReviewService {

	Page<ReviewResponseDto> getReviews(Long product_id, int page, boolean isAsc);

	ReviewResponseDto createReview(Long product_id, ReviewRequestDto requestDto, long memberId);

	ReviewResponseDto readView(Long product_id, Long review_id, long memberId);

	ReviewResponseDto updateReview(Long product_id, Long review_id, ReviewEditRequestDto requestDto, long memberId);

	void deleteReview(Long product_id, Long review_id, long memberId);

	void deleteChildReview(Long product_id, Long review_id, Long childReviewId, long memberId);

}
