package com.backsuend.coucommerce.order.service;

/**
 * @author rua
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderProduct;

import lombok.RequiredArgsConstructor;

/**
 * 주문 스냅샷 생성 서비스
 * - 주문 시점의 Product.price를 OrderProduct.priceSnapshot으로 고정
 * - Product 참조를 보존(이름은 Product에서 노출, 별도의 name 스냅샷은 현재 스키마 미보유)
 * - N+1 제거: 스냅샷 생성 시 상품을 일괄 조회
 */
@Service
@RequiredArgsConstructor
public class OrderSnapshotService {

	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public List<OrderProduct> toOrderProducts(Order order, List<CartItem> cartItems) {
		Objects.requireNonNull(order, "order must not be null");
		if (cartItems == null || cartItems.isEmpty())
			return Collections.emptyList();

		// 1) 기본 유효성
		for (CartItem c : cartItems) {
			if (c.getProductId() == null || c.getQuantity() <= 0) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 카트 항목: " + c);
			}
		}

		// 2) 일괄 조회
		List<Long> ids = cartItems.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
		List<Product> products = productRepository.findAllById(ids);
		Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

		// 존재하지 않는 상품 방어
		List<Long> missing = ids.stream().filter(id -> !productMap.containsKey(id)).collect(Collectors.toList());
		if (!missing.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 상품: " + missing);
		}

		// 3) 매핑
		List<OrderProduct> result = new ArrayList<>();
		for (CartItem c : cartItems) {
			Product p = productMap.get(c.getProductId());
			OrderProduct op = new OrderProduct();
			op.setOrder(order);
			op.setProduct(p);
			op.setQuantity(c.getQuantity());
			op.setPriceSnapshot(p.getPrice()); // 주문 시점 가격 스냅샷
			result.add(op);
		}
		return result;
	}
}
