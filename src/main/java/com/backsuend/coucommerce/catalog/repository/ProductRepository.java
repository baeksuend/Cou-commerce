package com.backsuend.coucommerce.catalog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	/** TEST admin 상세내용 **/
	Optional<Product> findById(long id);

	/** SELLER_READ - SELLER 상세내용보기 **/
	Optional<Product> findByDeletedAtIsNullAndIdAndMember_Id(long productId, long memberId);

	@Query("""
			SELECT p FROM Product p
			LEFT JOIN p.productSummary ps
			LEFT JOIN fetch p.productThumbnails
			WHERE p.deletedAt is null and p.member =:member
			  and p.id = :productId
		""")
	Optional<Product> findSellerRead(Member member, long productId);

	/** USER_READ - BUYER 상세내용 **/
	@Query("""
			SELECT p FROM Product p
			LEFT JOIN p.productSummary ps
			LEFT JOIN fetch p.productThumbnails
			WHERE p.deletedAt is null
			  and p.id = :productId
		""")
	Optional<Product> findUserRead(long productId);

	/** Main List - 메인 인기상품 목록 / 주문수, 찜  **/
	@Query(value = """
		SELECT p.*  FROM product p LEFT JOIN product_summary ps ON p.id = ps.product_id
		WHERE p.deletedAt IS NULL AND p.is_status = true
		ORDER BY ps.order_count DESC, ps.zim_count DESC
		""",
		countQuery = """
			SELECT COUNT(*) FROM product p LEFT JOIN product_summary ps ON p.id = ps.product_id
			WHERE p.deletedAt IS NULL AND p.is_status = true
			""",
		nativeQuery = true)
	Page<Product> searchMainBestProducts(Pageable pageable);

	/** Main List2 - 메인 리뷰 많은순 목록**/
	@Query(value = """
		SELECT p.*  FROM product p LEFT JOIN product_summary ps ON p.id = ps.product_id
		WHERE p.deletedAt IS NULL AND p.is_status = true
		ORDER BY ps.review_count DESC
		""",
		countQuery = """
			SELECT COUNT(*) FROM product p LEFT JOIN product_summary ps ON p.id = ps.product_id
			WHERE p.deletedAt IS NULL AND p.is_status = true
			""",
		nativeQuery = true)
	Page<Product> searchMainManyReviewProducts(Pageable pageable);

	/** user category RECENT 최신순(createdAt desc ) **/
	@Query("""
			SELECT p FROM Product p LEFT JOIN p.productSummary ps WHERE p.deletedAt is null
			  and p.visible = true and p.category = :cate and (:keyword is null or p.name like concat('%', :keyword, '%'))
			order by p.createdAt desc
		""")
	Page<Product> userListCategory_RECENT(Category cate, String keyword, Pageable pageable);

	/** user category LOW_PRICE 저가순(price asc) **/
	@Query("""
			SELECT p FROM Product p LEFT JOIN p.productSummary ps WHERE p.deletedAt is null
			  and p.visible = true and p.category = :cate and (:keyword is null or p.name like concat('%', :keyword, '%'))
			order by p.price asc
		""")
	Page<Product> userListCategory_LOW_PRICE(Category cate, String keyword, Pageable pageable);

	/** user category HIGH_PRICE 고가순(price desc) **/
	@Query("""
			SELECT p FROM Product p LEFT JOIN p.productSummary ps WHERE p.deletedAt is null
			  and p.visible = true and p.category = :cate and (:keyword is null or p.name like concat('%', :keyword, '%'))
			order by p.price desc
		""")
	Page<Product> userListCategory_HIGH_PRICE(Category cate, String keyword, Pageable pageable);

	/** user category SALE_COUNT_TOTAL 판매순(user category LOW_PRICE 고가순(price desc) **/
	@Query("""
			SELECT p FROM Product p LEFT JOIN p.productSummary ps WHERE p.deletedAt is null
			  and p.visible = true and p.category = :cate and (:keyword is null or p.name like concat('%', :keyword, '%'))
			order by ps.orderCount desc, ps.viewCount desc, ps.zimCount desc
		""")
	Page<Product> userListCategory_SALE_COUNT_TOTAL(Category cate, String keyword, Pageable pageable);

	/** user category SALE_COUNT_TOTAL 리뷰점수순(avgReviewScore desc) **/
	@Query("""
			SELECT p FROM Product p LEFT JOIN p.productSummary ps WHERE p.deletedAt is null
			  and p.visible = true and p.category = :cate and (:keyword is null or p.name like concat('%', :keyword, '%'))
			order by ps.reviewAvgScore desc
		""")
	Page<Product> userListCategory_REVIEW_SCORE_TOTAL(Category cate, String keyword, Pageable pageable);

	/** LIST - SELLER_LIST_ALL 판매자 목록 **/
	@Query("""
			SELECT p FROM Product p LEFT JOIN p.productSummary ps WHERE p.deletedAt is null and p.member=:member
			and (:cate is null or p.category =:cate)
			and (:keyword is null or p.name like concat('%', :keyword, '%'))
			order by p.createdAt desc
		""")
	Page<Product> sellerListCategory_RECENT(Member member, String keyword, Category cate, Pageable pageable);

}