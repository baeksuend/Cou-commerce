package com.backsuend.coucommerce.catalog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.catalog.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	//admin 상세내용
	Optional<Product> findById(long id);

	//admin 상세내용
	Optional<Product> findByDeletedAtIsNullAndId(long id);

	//seller 상세내용보기
	@Query(value = "Select * from product p where deletedAt is null "
		+ " and (id =:id and member_id =:memberId) ", nativeQuery = true)
	Optional<Product> findByDeletedAtIsNullAndIdAndMemberId(long id, long memberId);

	//user 상세내용
	@Query(value = "Select * from product p where deletedAt is null and is_status=true "
		+ " and (id =:id) ", nativeQuery = true)
	Optional<Product> findByDeletedAtIsNullAndVisibleIsTrueAndId(long id);

	//ADMIN_LIST_ALL 목록
	@Query(value = "Select * from product p where id is not null "
		+ " and (member_id = :memberId name like CONCAT('%',:keyword,'%') or category = :cate) ",
		nativeQuery = true)
	Page<Product> findByIdIsNotNullAndMemberIdOrNameOrCategory(long memberId,
		@Param("keyword") String keyword,
		@Param("cate") String cate,
		Pageable pageable);

	//SELLER_LIST_ALL 목록
	@Query(value = "Select * from product p where deletedAt is null and member_id=:memberId "
		+ " and ( name like CONCAT('%',:keyword,'%') or category = :cate) ", nativeQuery = true)
	Page<Product> findByMemberIdAndNameOrCategory(long memberId,
		@Param("keyword") String keyword,
		@Param("cate") String cate,
		Pageable pageable);

	//USER_LIST_ALL 목록
	@Query(value = "Select * from product p where  deletedAt is null and is_status=true "
		+ " and (:keyword is null or name like CONCAT('%',:keyword,'%'))"
		+ " and (:cate is null or category =:cate)", nativeQuery = true)
	Page<Product> findByVisibleIsTrueAndNameOrCategory(String keyword, String cate,
		Pageable pageable);

	//USER_LIST_CATEGORY 카테고리별 목록
	@Query(value = "Select * from product p where  deletedAt is null and is_status=true "
		+ " and category =:cate "
		+ " and (:keyword is null or name like CONCAT('%',:keyword,'%')) ", nativeQuery = true)
	Page<Product> findByVisibleIsTrueAndCategoryOrNameContaining(String cate,
		String keyword, Pageable pageable);

}