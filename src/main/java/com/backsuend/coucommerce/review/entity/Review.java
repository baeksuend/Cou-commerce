package com.backsuend.coucommerce.review.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.common.entity.BaseTimeEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@ToString
@EntityListeners(AuditingEntityListener.class)
@Table(name = "review",
	indexes = {
		@Index(name = "idx_review_member", columnList = "member_id"),
		@Index(name = "idx_review_product", columnList = "product_id")
	})
public class Review extends BaseTimeEntity {

	@Schema(description = "리뷰 아이디", example = "3")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Schema(description = "제품 아이디", example = "2")
	@ManyToOne(cascade = CascadeType.PERSIST)
	//@ManyToOne(fetch=FetchType.LAZY) // 이 엔티티(Comment)와 부모 엔티티(Article)를 다대일 관계로 설정
	@JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false, updatable = false)
	private Product product;

	@Schema(description = "회원 아이디", example = "2")
	@ManyToOne(cascade = CascadeType.PERSIST)//@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "member_id", referencedColumnName = "id", updatable = false)
	private Member member;

	@Schema(description = "리뷰 내용", example = "상세내용입니다.")
	@Column
	private String content;

	@Schema(description = "부모 댓글 여부 확인", example = "상세내용입니다.")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_review_id", updatable = false)
	private Review parentReview;

	@Schema(description = "대댓글 목록")
	@OneToMany(mappedBy = "parentReview", orphanRemoval = true)
	private List<Review> childReviews = new ArrayList<>();

	@Schema(description = "부모 댓글 삭제 상태")
	@ColumnDefault("FALSE")
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	/* 대댓글 조회용*/
	@Builder
	public Review(Long id, Member member, Product product, String content, Review parentReview,
		LocalDateTime createdAt) {
		this.id = id;
		this.member = member;
		this.product = product;
		this.content = content;
		this.parentReview = parentReview;
		this.createdAt = createdAt;
	}

	public void markAsDeleted() {
		this.isDeleted = true;
		this.updateReview("삭제된 댓글입니다");
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void updateReview(String content) {
		this.content = content;
	}
}
