package com.backsuend.coucommerce.catalog.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.entity.ProductThumbnail;
import com.backsuend.coucommerce.catalog.enums.Category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ProductResponse {

	@Schema(description = "상품 아이디", example = "1")
	private Long id;

	@Schema(description = "상품명", example = "우리 현미쌀")
	private String name;

	@Schema(description = "상품내용", example = "상품내용입니다.")
	private String detail;

	@Schema(description = "재고수량", example = "50")
	private int stock;

	@Schema(description = "가격", example = "40")
	private int price;

	@Schema(description = "카테고리", example = "BOOKS")
	private Category category;

	@Schema(description = "진열여부", example = "true")
	private LocalDateTime createdAt;

	private String productThumbnail_S; // 작은 이미지
	private String productThumbnail_M; // 작은 이미지
	private String productThumbnail_L; // 작은 이미지

	public static ProductResponse fromEntity(Product product) {

		List<ProductThumbnail> thumbnails = Optional.ofNullable(product.getProductThumbnails())
			.orElse(Collections.emptyList());

		return new ProductResponse(
			product.getId(),
			product.getName(),
			product.getDetail(),
			product.getStock(),
			product.getPrice(),
			product.getCategory(),
			product.getCreatedAt(),
			thumbnails.stream()
				.filter(p -> "S".equals(p.getImageType()))
				.map(ProductThumbnail::getImagePath).findFirst().orElse(""),
			thumbnails.stream()
				.filter(p -> "M".equals(p.getImageType()))
				.map(ProductThumbnail::getImagePath).findFirst().orElse(""),
			thumbnails.stream()
				.filter(p -> "L".equals(p.getImageType()))
				.map(ProductThumbnail::getImagePath).findFirst().orElse("")
		);
	}

}
