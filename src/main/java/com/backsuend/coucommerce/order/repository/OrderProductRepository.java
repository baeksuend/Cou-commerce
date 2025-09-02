package com.backsuend.coucommerce.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.order.entity.OrderProduct;

/**
 * @author rua
 */

@Repository
public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
	// 특정 주문의 모든 상품 조회
	List<OrderProduct> findByOrderId(Long orderId);

	// 특정 상품이 포함된 주문 조회 (Seller가 자신의 상품 주문 확인)
	List<OrderProduct> findByProductId(Long productId);
}