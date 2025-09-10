package com.backsuend.coucommerce.order.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderStatus;

/**
 * @author rua
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	// Buyer 입장에서 내 주문 목록 조회
	Page<Order> findByMemberId(Long memberId, Pageable pageable);

	@Query("select distinct o from Order o join o.items i where i.product.member.id = :sellerId")
	List<Order> findBySellerId(@Param("sellerId") Long sellerId);

	@Query("""
		select distinct o from Order o
		join o.items i
		where (:buyerId is null or o.member.id = :buyerId)
		  and (:sellerId is null or i.product.member.id = :sellerId)
		  and (:status is null or o.status = :status)
		""")
	List<Order> findAllByFilters(
		@Param("buyerId") Long buyerId,
		@Param("sellerId") Long sellerId,
		@Param("status") OrderStatus status
	);
}
