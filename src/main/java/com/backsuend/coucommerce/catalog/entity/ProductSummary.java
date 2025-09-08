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

import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product_summary")
public class ProductSummary extends BaseTimeEntity {

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
	private int viewCount = 0;

	@Schema(description = "구매 수량", example = "1")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "order_count")
	private int orderCount = 0;

	@Schema(description = "찜", example = "2")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "zim_count")
	private int zimCount = 0;

	@Schema(description = "리뷰 개수", example = "3")
	@Digits(integer = 10, fraction = 0, message = "숫자만 입력가능합니다.")
	@Column(name = "review_count")
	private int reviewCount = 0;

	@Schema(description = "평균 총점", example = "10.5")
	//@Digits(integer = 10, fraction = 1, message = "숫자만 입력가능합니다.")
	@Column(name = "review_total_score")
	private double reviewTotalScore = 0;

	@Schema(description = "평균 평점", example = "3.5")
	//@Digits(integer = 10, fraction = 1, message = "숫자만 입력가능합니다.")
	@Column(name = "review_avg_score")
	private double reviewAvgScore = 0;

}
