package com.backsuend.coucommerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProductThumbnailDto {

	@Schema(description = "이미지 경로", example = "/image/product/")
	private String imagePath;

	@Schema(description = "이미지 형태", example = "S")
	private String imageType;
}
