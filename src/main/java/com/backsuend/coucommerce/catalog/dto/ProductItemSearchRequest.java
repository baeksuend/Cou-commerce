package com.backsuend.coucommerce.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.backsuend.coucommerce.catalog.enums.ProductSortType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class ProductItemSearchRequest {

	@Schema(description = "페이지번호", example = "1")
	@NotNull
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	private Integer page = 1;

	@Schema(description = "한페이지 데이터 갯수", example = "10", nullable = true)
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	private Integer pageSize = 10;

	@Schema(description = "정렬이름")
	@NotNull(message = "카테고리는 필수입니다.")
	@Builder.Default
	private ProductSortType sort = ProductSortType.RECENT;

/*	@Schema(description = "정렬순서", example = "asc")
	@Pattern(regexp = "^(asc|desc)$", flags = Pattern.Flag.CASE_INSENSITIVE,
		message = "정렬 방향은 asc 또는 desc만 가능합니다.")
	private String sortDir;*/

	@Schema(description = "검색어")
	private String keyword;

	public int getPage() {
		return page != null ? page : 1;
	}

	public Integer getPageSize() {
		return pageSize != null ? pageSize : 10;
	}

}
