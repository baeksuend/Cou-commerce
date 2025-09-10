package com.backsuend.coucommerce.sellerregistration.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.backsuend.coucommerce.auth.entity.QMember;
import com.backsuend.coucommerce.sellerregistration.dto.SellerRegistrationSearchRequest;
import com.backsuend.coucommerce.sellerregistration.entity.QSellerRegistration;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SellerRegistrationRepositoryImpl implements SellerRegistrationRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<SellerRegistration> search(SellerRegistrationSearchRequest request, Pageable pageable) {
		QSellerRegistration sellerRegistration = QSellerRegistration.sellerRegistration;
		QMember member = QMember.member;

		BooleanBuilder builder = new BooleanBuilder();

		// Filter by status
		if (request.getStatus() != null) {
			builder.and(sellerRegistration.status.eq(request.getStatus()));
		}

		// Filter by storeName
		if (StringUtils.hasText(request.getStoreName())) {
			builder.and(sellerRegistration.storeName.containsIgnoreCase(request.getStoreName()));
		}

		// Filter by businessRegistrationNumber
		if (StringUtils.hasText(request.getBusinessRegistrationNumber())) {
			builder.and(sellerRegistration.businessRegistrationNumber.eq(request.getBusinessRegistrationNumber()));
		}

		// Filter by memberEmail
		if (StringUtils.hasText(request.getMemberEmail())) {
			builder.and(sellerRegistration.member.email.containsIgnoreCase(request.getMemberEmail()));
		}

		// Filter by memberName
		if (StringUtils.hasText(request.getMemberName())) {
			builder.and(sellerRegistration.member.name.containsIgnoreCase(request.getMemberName()));
		}

		JPAQuery<SellerRegistration> query = queryFactory.selectFrom(sellerRegistration)
			.leftJoin(sellerRegistration.member, member).fetchJoin()
			.where(builder);

		// Apply sorting
		List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
		if (StringUtils.hasText(request.getSortBy())) {
			if ("createdAt".equals(request.getSortBy())) {
				orderSpecifiers.add(
					"asc".equalsIgnoreCase(request.getSortDirection()) ? sellerRegistration.createdAt.asc()
						: sellerRegistration.createdAt.desc());
			} else if ("storeName".equals(request.getSortBy())) {
				orderSpecifiers.add(
					"asc".equalsIgnoreCase(request.getSortDirection()) ? sellerRegistration.storeName.asc()
						: sellerRegistration.storeName.desc());
			} else if ("memberEmail".equals(request.getSortBy())) {
				orderSpecifiers.add(
					"asc".equalsIgnoreCase(request.getSortDirection()) ? sellerRegistration.member.email.asc()
						: sellerRegistration.member.email.desc());
			}
			// Add more sortable fields as needed
		} else {
			// Default sort if no sortBy is provided
			orderSpecifiers.add(sellerRegistration.createdAt.desc());
		}
		query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));

		// Apply paging
		List<SellerRegistration> content = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory.select(sellerRegistration.count())
			.from(sellerRegistration)
			.where(builder);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}
}
