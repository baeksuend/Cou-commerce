package com.backsuend.coucommerce.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequest {

	@Schema(description = "회원 등록자 아이디", example = "1")
	@NotNull(message = "회원아이디는 필수입니다.")
	private Long member_id;

	@Schema(description = "상품명", example = "우리 현미쌀")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(min = 2, max = 50, message = "상품명은 필수입니다.")
	private String name;

	@Schema(description = "상품내용", example = "상품내용입니다.")
	@NotBlank(message = "상품내용은 필수입니다.")
	private String detail;

	@Schema(description = "재고수량", example = "50")
	@NotNull(message = "재고수량은 필수입니다.")
	private int stock;

	@Schema(description = "가격", example = "40")
	@NotNull(message = "가격은 필수입니다.")
	private int price;

	@Schema(description = "카테고리", example = "BOOKS")
	@NotNull(message = "카테고리는 필수입니다.")
	private Category category;

	@Schema(description = "진열여부", example = "true")
	private boolean visible;

	public Product toEntity(Member member) {
		return Product.builder()
			.member(member)
			.name(name)
			.detail(detail)
			.stock(stock)
			.price(price)
			.category(category)
			.visible(visible)
			.build();
	}

}
