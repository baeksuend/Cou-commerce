package com.backsuend.coucommerce.order.service;

/**
 * @author rua
 */

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductSummaryRepository;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderDetailProduct;

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

	private final ProductSummaryRepository productSummaryRepository;

	@Transactional(readOnly = true)
	public Order toOrderProducts(Order order, List<CartItem> cartItems,
		Map<Long, Product> productMap) {
		int totalPrice = 0;
		// cartItemsf를 사용해 OrderDetaileProduct 를 채워야 한다.
		for (CartItem item : cartItems) {
			Product product = productMap.get(item.getProductId());
			if (product == null)
				continue;
			OrderDetailProduct op = OrderDetailProduct.builder()
				.order(order)
				.product(product)
				.quantity(item.getQuantity()) // ✅ 주문자가 선택한 수량
				.priceSnapshot(product.getPrice())  // ✅ 주문 시점 가격 스냅샷
				.build();
			order.addItem(op);

            // 재고 차감 (낙관적 락)
            product.reduceStock(item.getQuantity());
			totalPrice += product.getPrice() * item.getQuantity();
		}

		order.setTotalPrice(totalPrice);
		return order;
	}
}
