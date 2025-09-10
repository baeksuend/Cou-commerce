package com.backsuend.coucommerce.catalog.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product",
	indexes = {
		@Index(name = "idx_product_member", columnList = "member_id"),
		@Index(name = "idx_product_category", columnList = "category"),
		@Index(name = "idx_product_visible", columnList = "is_status")
	})
public class Product extends BaseTimeEntity {

	@Schema(description = "상품 아이디", example = "1")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// SELLER 소유자
	@Schema(description = "판매자 아이디", example = "1")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Schema(description = "상품명", example = "사과")
	@NotBlank
	@Size(max = 50)
	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Schema(description = "상품상세정보", example = "<p>상세정보입니다.</p>")
	@Lob
	@NotBlank
	@Column(name = "detail", nullable = false)
	private String detail;

	@Schema(description = "재고수량", example = "20")
	@Min(0)
	@Column(name = "stock", nullable = false)
	private int stock;

	@Schema(description = "상품가격", example = "50000")
	@Min(0)
	@Column(name = "price", nullable = false)
	private int price;

	//*** 배송비 추가
	@Schema(description = "배송비", example = "5000")
	@Digits(integer = 5, fraction = 0, message = "숫자만 가능합니다.")
	@Column(name = "tran_price", nullable = false)
	private int tranPrice;

	@Schema(description = "상품분류", example = "FOOD")
	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 20)
	private Category category;

	/** 공개 여부 (is_status) */
	@Schema(description = "진열여부", example = "true")
	@Column(name = "is_status", nullable = false)
	private boolean visible;

	@Version
	private Long version;

	/**
	 *  summery 엔티티와 1:1 동일하게 세팅
	 * */
	@OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
	private ProductSummary productSummary;

	@Builder.Default
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductThumbnail> productThumbnails = new ArrayList<>();

	/** 삭제처리 **/
	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	/**재고 감소 + 유효성 검증**/
	public void reduceStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("차감할 수량은 0보다 커야 합니다.");
		}
		if (this.stock < quantity) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
				"재고가 부족합니다. 요청수량=" + quantity + ", 남은재고=" + this.stock);
		}
		this.stock -= quantity;
	}
}
