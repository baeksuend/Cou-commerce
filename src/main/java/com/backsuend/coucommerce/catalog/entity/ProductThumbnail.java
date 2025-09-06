package com.backsuend.coucommerce.catalog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor // 디폴트 생성자
@Table(name = "product_thumbnails")
public class ProductThumbnail {

	@Schema(description = "썸네일 아이디", example = "1")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // PK

	@Schema(description = "제품 아이디", example = "1")
	@ManyToOne
	@JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
	private Product product; // FK

	@Schema(description = "이미지 경로", example = "/image/product/")
	@Column(name = "image_path", nullable = false)
	private String imagePath;

	@Schema(description = "이미지 형태", example = "S")
	@Column(name = "image_type", nullable = false)
	private String imageType;

	@Schema(description = "등록일", example = "2025-05-25")
	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createdAt;

	// 생성자
	public ProductThumbnail(Product product, String imageType, String imagePath) {
		this.product = product;
		this.imagePath = imagePath;
		this.imageType = imageType;
		this.createdAt = LocalDateTime.now();

	}
}