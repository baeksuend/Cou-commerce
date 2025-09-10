package com.backsuend.coucommerce.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.catalog.entity.ProductSummary;

@Repository
public interface ProductSummaryRepository extends JpaRepository<ProductSummary, Long> {

	/**
	 * 조횟수 업데이트
	 * @param productId 상품아이디
	 */
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.view_count = p.view_count+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementViewCount(long productId);

	/**
	 * 주문횟수 업데이트
	 * @param productId 상품아이디
	 */
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.view_count = p.view_count + :count where p.product_id=:productId",
		nativeQuery = true)
	void incrementOrderCount(long productId, int count);

	/**
	 * 찜횟수 업데이트
	 * @param productId 상품아이디
	 */
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p Set p.zim_count = p.zim_count+1 where p.product_id=:productId",
		nativeQuery = true)
	void incrementLikeCount(long productId);

	/**
	 * 리뷰갯수, 총점, 평균 업데이트
	 * @param productId 상품아이디
	 */
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p "
		+ " Set p.review_count = p.review_count+1, "
		+ " review_total_score = review_total_score+ :newScore ,"
		+ " review_avg_score = (review_total_score+ :newScore)/(review_count+1)"
		+ " where p.product_id=:productId",
		nativeQuery = true)
	void incrementReviewCount(double newScore, long productId);

	/**
	 * 리뷰갯수, 총점, 평균 업데이트
	 * @param productId 상품아이디
	 */
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p "
		+ " Set  "
		+ " review_total_score = review_total_score+ :newScore ,"
		+ " review_avg_score = (review_total_score+ :newScore)/(review_count)"
		+ " where p.product_id=:productId",
		nativeQuery = true)
	void incrementReviewCountEdit(double newScore, long productId);

	/**
	 * 리뷰갯수, 총점, 평균 업데이트
	 * @param productId 상품아이디
	 */
	@Modifying
	@Transactional
	@Query(value = "Update product_summary p "
		+ " Set p.review_count = p.review_count-1, "
		+ " review_total_score = review_total_score-1 ,"
		+ " review_avg_score = (review_total_score-1)/(review_count-1)"
		+ " where p.product_id=:productId",
		nativeQuery = true)
	void incrementReviewCountDelete(long productId);

}
