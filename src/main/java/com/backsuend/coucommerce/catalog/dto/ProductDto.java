package com.backsuend.coucommerce.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ProductDto {
/*

	@Schema(description = "상품 아이디", example = "1")
	private Long id;

	@Schema(description = "회원 등록자 아이디", example = "1")
	private Long member_id;

	@Schema(description = "상품명", example = "우리 현미쌀")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(min = 2, max = 50, message = "상품명은 필수입니다.")
	private String name;

	@Schema(description = "상품내용", example = "상품내용입니다.")
	@NotBlank(message = "상품내용은 필수입니다.")
	private String detail;

	@Schema(description = "재고수량", example = "10")
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

	@Schema(description = "등록일", example = "2025-03-04T00:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "수정일")
	private LocalDateTime updatedAt;

	@Schema(description = "삭제일")
	private LocalDateTime deletedAt;

	public static ProductDto fromEntity(Product product) {
		return new ProductDto(
			product.getId(),
			product.getMember().getId(),
			product.getName(),
			product.getDetail(),
			product.getStock(),
			product.getPrice(),
			product.getCategory(),
			product.isVisible(),
			product.getCreatedAt(),
			product.getUpdatedAt(),
			product.getDeletedAt()
		);
	}

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
*/

}
