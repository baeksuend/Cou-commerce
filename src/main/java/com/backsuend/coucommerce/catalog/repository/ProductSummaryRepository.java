package com.backsuend.coucommerce.catalog.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.catalog.entity.ProductSummary;

@Repository
public interface ProductSummaryRepository extends CrudRepository<ProductSummary, Long> {

	//조횟수 업데이트
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.view_count = p.view_count+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementViewCount(long productId);

	//주문횟수 업데이트
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.view_count = p.view_count+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementOrderCount(long productId);

	//찜횟수 업데이트
	@Modifying
	@Transactional
	@Query(value = "Update product p Set p.zim_count = p.zim_count+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementLikeCount(long productId);

	//리뷰갯수 업데이트
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.review_count = p.review_count+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementReviewCount(long productId);

	//리뷰 평점 업데이트
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.avg_reviw_score = p.avg_reviw_score+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementAveReviewCount(long productId);

}
