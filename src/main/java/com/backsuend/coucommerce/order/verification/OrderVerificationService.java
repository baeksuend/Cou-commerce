package com.backsuend.coucommerce.order.verification;

/**
 * @author rua
 */

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

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
public class OrderVerificationService {

	private final ProductRepository productRepository;

	public OrderVerificationService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public void verify(List<CartItem> items) {
		Objects.requireNonNull(items, "cart items must not be null");

		if (items.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "장바구니가 비어있습니다.");
		}

		for (CartItem item : items) {
			final Long productId = item.getProductId();
			final Integer qty = item.getQuantity();
			final Integer priceAtAdd = item.getPrice(); // 장바구니에 담을 당시 가격 스냅샷

			if (productId == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "상품 ID가 없습니다.");
			}
			if (qty == null || qty <= 0) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "수량은 1 이상이어야 합니다. productId=" + productId);
			}

			final Product product = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품 없음: " + productId));

			if (product.getStock() < qty) {
				throw new BusinessException(
					ErrorCode.CONFLICT,
					"재고 부족: productId=" + productId + ", 요청수량=" + qty + ", 보유재고=" + product.getStock()
				);
			}

			if (priceAtAdd != null && product.getPrice() != priceAtAdd) {
				throw new BusinessException(
					ErrorCode.CONFLICT,
					"가격 변동: productId=" + productId + ", 장바구니가격=" + priceAtAdd + ", 현재가격=" + product.getPrice()
				);
			}
		}
	}
}
