package com.backsuend.coucommerce.order.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

	// 상태별 주문 조회 (Seller/Admin 용도로도 확장 가능)
	List<Order> findByStatus(OrderStatus status);
}
