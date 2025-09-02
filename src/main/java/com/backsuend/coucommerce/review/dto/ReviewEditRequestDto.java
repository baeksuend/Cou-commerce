package com.backsuend.coucommerce.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewEditRequestDto {

	@Schema(description = "리뷰 아이디", example = "3")
	private Long id;

	@Schema(description = "제품 아이디", example = "3")
	private Long product_id;

	@Schema(description = "리뷰내용", example = "리뷰내용입니다.")
	private String content;

	@Schema(description = "부모 아이디값", example = "4.")
	private Long parent_review_id;

	@Builder
	public ReviewEditRequestDto(Long id, Long product_id,
		String content, Long parent_review_id) {
		this.id = id;
		this.product_id = product_id;
		this.content = content;
		this.parent_review_id = parent_review_id;
	}
}
