package com.backsuend.coucommerce.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.review.entity.QReview;
import com.backsuend.coucommerce.review.entity.Review;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReviewRespositoryImpl implements ReviewCustomRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Review> findByProductAndId(Product product, Long review_id) {
		QReview review = QReview.review;
		Review result = queryFactory
			.selectFrom(review)
			.where(review.product.eq(product), review.id.eq(review_id))
			.fetchOne();
		return Optional.ofNullable(result);
	}

	@Override
	public Page<Review> findByProductAndParentReviewIsNull(Product product, Pageable pageable) {
		QReview review = QReview.review;

		//* 부모 댓글이 null 조건을 추가하여 QueryDSL 쿼리 구성
		JPAQuery<Review> query = queryFactory
			.selectFrom(review)
			.where(review.product.eq(product)
				.and(review.parentReview.isNull()));

		//* 페이지네이션을 위한 정렬 및 페이징 적용
		Sort.Order sortOrder = pageable.getSort().isSorted() ? pageable.getSort().iterator().next() : null;
		OrderSpecifier<?> orderBySpecifier = sortOrder != null
			? (sortOrder.isAscending() ? review.createdAt.asc() : review.createdAt.desc())
			: review.createdAt.asc();
		query.orderBy(orderBySpecifier);

		//* 페이징 처리를 적용하여 결과 조회
		List<Review> results = query
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		// 전체 개수 조회를 위한 쿼리
		//* fetchCount() deprecated 로 인해 사용 불가
		Long total = query.select(review.count())
			.from(review)
			.where(review.product.eq(product)
				.and(review.parentReview.isNull()))
			.fetchOne();

		//* null 체크를 통해 NullPointerException 방지
		long totalCount = total != null ? total : 0L;

		return new PageImpl<>(results, pageable, totalCount);

	}
}
