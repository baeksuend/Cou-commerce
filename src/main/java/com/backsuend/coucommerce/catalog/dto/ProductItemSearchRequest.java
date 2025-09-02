package com.backsuend.coucommerce.catalog.dto;

import com.backsuend.coucommerce.catalog.enums.Category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductItemSearchRequest {

	@Schema(description = "페이지번호", example = "3")
	private Integer page = 1;

	@Schema(description = "한페이지 데이터 갯수", example = "3", nullable = true)
	private Integer pageSize = 10;

	@Schema(description = "정렬이름", example = "name")
	private String sort;

	@Schema(description = "정렬순서", example = "asc")
	private String sortDir;

	@Schema(description = "검색어", example = "책", nullable = true)
	private String keyword;

	@Schema(description = "검색분류", example = "카테고리 분류", nullable = true)
	private Category cate;

	public Integer getPage() {
		return page != null ? page : 1;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getPageSize() {
		return pageSize != null ? pageSize : 10;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

}
