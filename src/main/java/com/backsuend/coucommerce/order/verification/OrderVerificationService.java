package com.backsuend.coucommerce.order.verification;

/**
 * @author rua
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 5.1 가격/재고 재검증 (CartItem 사용 버전)
 * - 주문 직전 Cart의 각 항목을 Product DB 최신 상태와 대조하여 정합성을 보장합니다.
 * - 정책:
 *   * 상품 미존재  -> NOT_FOUND
 *   * 수량 <= 0   -> INVALID_REQUEST
 *   * 재고 부족   -> CONFLICT(409)
 *   * 가격 변동   -> CONFLICT(409) (CartItem.price와 Product.price 비교)
 */
@Service
@RequiredArgsConstructor
public class OrderVerificationService {

	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public Map<Long, Product> verify(List<CartItem> items) {
		Map<Long, Product> productMap = new HashMap<>();
		for (CartItem item : items) {
			Product product = productRepository.findById(item.getProductId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품 없음",
					Map.of("productId", item.getProductId())));
			if (!product.isVisible()) {
				throw new BusinessException(ErrorCode.CONFLICT, "판매 중단된 상품",
					Map.of("productId", product.getId()));
			}
			if (product.getStock() < item.getQuantity()) {
				throw new BusinessException(ErrorCode.CONFLICT, "재고 부족",
					Map.of("productId", product.getId(), "stock", product.getStock()));
			}
			if (product.getPrice() != item.getPriceAtAdd()) {
				throw new BusinessException(ErrorCode.CONFLICT, "가격 변동",
					Map.of("productId", product.getId(),
						"expectedPrice", product.getPrice(),
						"cartPrice", item.getPriceAtAdd()));
			}
			productMap.put(product.getId(), product);
		}
		return productMap;
	}

}
