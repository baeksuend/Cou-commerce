package com.backsuend.coucommerce.review.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.backsuend.coucommerce.review.entity.Review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ReviewResponseDto {

	@Schema(description = "제품 아이디", example = "3")
	private Long id;

	@Schema(description = "제품 아이디", example = "3")
	private Long productId;

	@Schema(description = "회원 아이디", example = "3")
	private Long memberId;

	@Schema(description = "회원이름", example = "3")
	private String memberName;

	@Schema(description = "리뷰내용", example = "리뷰내용입니다.")
	private String content;

	@Schema(description = "리뷰평점", example = "3.5")
	private int avgReviewScore;

	@Schema(description = "등록일", example = "2025-08-02T00:00:00")
	private LocalDateTime createdAt;
	private List<ReviewResponseDto> childComments = new ArrayList<>();

	@Builder
	public ReviewResponseDto(Review review) {
		this.id = review.getId();
		this.productId = review.getProduct().getId();
		this.memberId = review.getMember().getId();
		this.memberName = review.getMember().getName();
		this.content = review.getContent();
		this.createdAt = review.getCreatedAt();
	}

	@Builder
	public ReviewResponseDto(Review review, List<ReviewResponseDto> childReviews) {
		this.id = review.getId();
		this.productId = review.getProduct().getId();
		this.memberId = review.getMember().getId();
		this.memberName = review.getMember().getName();
		this.content = review.getContent();
		this.createdAt = review.getCreatedAt();
		this.childComments = childReviews; /* 대댓글 목록 할당*/
	}
/*
	@Builder
	public ReviewResponseDto() {

	}

	public ReviewResponseDto(Long id, Long product_id, Long member_id, String memberName, String content,
		LocalDateTime createdAt) {
		this.id = id;
		this.product_id = product_id;
		this.member_id = member_id;
		this.memberName = memberName;
		this.content = content;
		this.createdAt = createdAt;
	}*/

}
