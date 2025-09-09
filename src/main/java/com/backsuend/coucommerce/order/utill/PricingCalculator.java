package com.backsuend.coucommerce.order.utill;

/**
 * @author rua
 */

import java.util.List;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.order.entity.OrderProduct;

/**
 * 총액/수량 계산 유틸 - PricingCalculator
 * - 금액/수량 계산을 서비스/컨트롤러에서 분리하여 재사용/검증 용이성 확보
 * - null/음수 방어 로직 포함
 */
public final class PricingCalculator {

	private PricingCalculator() {
	}

	// ---------- OrderProduct 기반 ----------

	public static int subtotal(OrderProduct op) {
		if (op == null)
			return 0;
		return safeMul(op.getPriceSnapshot(), op.getQuantity());
	}

	public static int totalAmount(List<OrderProduct> items) {
		if (items == null || items.isEmpty())
			return 0;
		int sum = 0;
		for (OrderProduct op : items) {
			sum += subtotal(op);
		}
		return sum;
	}

	public static int totalQuantity(List<OrderProduct> items) {
		if (items == null || items.isEmpty())
			return 0;
		int q = 0;
		for (OrderProduct op : items) {
			q += safeQty(op == null ? null : op.getQuantity());
		}
		return q;
	}

	// ---------- CartItem 기반 (필요 시 장바구니 단계에서도 활용) ----------

	public static int subtotal(CartItem ci) {
		if (ci == null)
			return 0;
		return safeMul(ci.getPrice(), ci.getQuantity());
	}

	public static int totalAmountFromCart(List<CartItem> items) {
		if (items == null || items.isEmpty())
			return 0;
		int sum = 0;
		for (CartItem ci : items) {
			sum += subtotal(ci);
		}
		return sum;
	}

	public static int totalQuantityFromCart(List<CartItem> items) {
		if (items == null || items.isEmpty())
			return 0;
		int q = 0;
		for (CartItem ci : items) {
			q += safeQty(ci == null ? null : ci.getQuantity());
		}
		return q;
	}

	// ---------- 내부 도우미 ----------

	private static int safeMul(Integer price, Integer qty) {
		int p = safePrice(price);
		int q = safeQty(qty);
		long mult = (long)p * (long)q; // overflow 방지 1차
		if (mult > Integer.MAX_VALUE)
			return Integer.MAX_VALUE; // 단순 클램프 (정책에 맞게 수정 가능)
		return (int)mult;
	}

	private static int safePrice(Integer price) {
		if (price == null || price < 0)
			return 0;
		return price;
	}

	private static int safeQty(Integer qty) {
		if (qty == null || qty < 0)
			return 0;
		return qty;
	}
}
