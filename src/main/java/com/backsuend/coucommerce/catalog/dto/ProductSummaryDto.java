package com.backsuend.coucommerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class ProductSummaryDto {

	@Schema(description = "상품아이디", example = "1")
	private Long productId;

	@Schema(description = "조회수", example = "412")
	private int viewCount;

	@Schema(description = "구매 수량", example = "1")
	private int orderCount;

	@Schema(description = "찜하기", example = "2")
	private int zimCount;

	@Schema(description = "리뷰 개수", example = "3")
	private int reviewCount;

	@Schema(description = "평균 평점", example = "3.5")
	private int avgReviewScore;

}
