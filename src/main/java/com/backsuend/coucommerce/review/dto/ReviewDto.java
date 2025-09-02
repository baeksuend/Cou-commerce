package com.backsuend.coucommerce.review.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReviewDto {

	@Schema(description = "리뷰 아이디", example = "3")
	private Long id;

	@Schema(description = "제품 아이디", example = "3")
	private long product_id;

	@Schema(description = "사용자 아이디", example = "1")
	@NotNull(message = "아이디는 필수입니다.")
	private Long member_id;

	@Schema(description = "리뷰내용", example = "리뷰내용입니다.")
	@NotBlank(message = "내용은 필수입니다.")
	private String content;

	@Schema(description = "등록일", example = "2025-08-02T00:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "수정일", example = "2025-08-02T00:00:00")
	private LocalDateTime updatedAt;

	@Schema(description = "삭제일", example = "2025-08-02T00:00:00")
	private LocalDateTime deletedAt;

	public ReviewDto() {
	}

}

