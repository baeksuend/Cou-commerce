package com.backsuend.coucommerce.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "product_summary")
public class ProductSummary {

	@Schema(description = "상품요약정보 아이디", example = "1")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Schema(description = "상품 아이디", example = "1")
	@OneToOne
	@MapsId
	@JoinColumn(name = "product_id")
	private Product product;

	@Schema(description = "조회수", example = "412")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "view_count")
	private int viewCount;

	@Schema(description = "구매 수량", example = "1")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "order_count")
	private int orderCount;

	@Schema(description = "찜", example = "2")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "zim_count")
	private int zimCount;

	@Schema(description = "리뷰 개수", example = "3")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "review_count")
	private int reviewCount;

	@Schema(description = "평균 평점", example = "3.5")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "avg_reviw_score")
	private int avgReviewScore;

}


