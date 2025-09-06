package com.backsuend.coucommerce.review.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ReviewRequestDto {

	@Schema(description = "리뷰내용", example = "리뷰내용입니다.")
	@NotBlank(message = "리뷰내용은 필수입니다.")
	private String content;

	@Schema(description = "부모 아이디값", example = "2", nullable = true)
	@Digits(fraction = 1, integer = 1, message = "숫자만 가능합니다.")
	private Long parentReviewId;

	@Schema(description = "평점", example = "4")
	@Digits(integer = 1, fraction = 1, message = "숫자만 가능합니다.")
	@Size(min = 1, max = 5, message = "상품명은 필수입니다.")
	@NotNull(message = "재고수량은 필수입니다.")
	private int avgReviewScore;

	@Builder
	public ReviewRequestDto(String content, int avgReviewScore, Long parentReviewId) {
		this.content = content;
		this.avgReviewScore = avgReviewScore;
		this.parentReviewId = parentReviewId;
	}
}
