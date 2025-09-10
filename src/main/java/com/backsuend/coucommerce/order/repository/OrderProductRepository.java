package com.backsuend.coucommerce.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.order.entity.OrderDetailProduct;

/**
 * @author rua
 */

@Repository
public interface OrderProductRepository extends JpaRepository<OrderDetailProduct, Long> {
}