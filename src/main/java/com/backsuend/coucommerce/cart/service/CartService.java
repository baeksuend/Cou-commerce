package com.backsuend.coucommerce.cart.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.backsuend.coucommerce.cart.config.CartRedisConfig;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

/**
 * ToDo
 * 1. 현재 add 및 update 할 시에 접근하는 항목에만 장바구니의 데이터들의 ttl이 7일로 다시 설정됨. 전부 설정하고 싶으면 변경 필요.
 * Done
 * 1. 해결됨. 30일로 사용자의 조회를 제외한 모든 접근에 항목을 업데이트 하도록 변경.
 * */

@Service
@RequiredArgsConstructor
public class CartService {
	/** Cart TTL: 30일 (PRD 기준). CartRedisConfig.CART_TTL 참조 */
	private static final Duration CART_TTL = CartRedisConfig.CART_TTL;
	@Qualifier("cartRedisTemplate")
	private final RedisTemplate<String, CartItem> cartRedisTemplate;

	private String getCartKey(Long memberId) {
		return "cart:" + memberId;
	}

	/** 쓰기 경로에서 TTL 보장 (슬라이딩 on write) */
	private void ensureTtl(Long memberId) {
		try {
			String key = getCartKey(memberId);
			cartRedisTemplate.expire(key, CART_TTL);
		} catch (Exception ignore) {
			// TTL 보장은 베스트에포트. 실패시 기능에는 영향 주지 않음.
		}
	}

	/** 장바구니 조회 */
	public CartResponse getCart(Long memberId) {
		String key = getCartKey(memberId);
		try {
			// 제네릭을 명시해 entries가 깔끔하게 Map<String, CartItem>로 나오게
			HashOperations<String, String, CartItem> hashOps = cartRedisTemplate.opsForHash();
			Map<String, CartItem> entries = hashOps.entries(key);

			// values() 바로 리스트로 변환
			List<CartItem> items = new ArrayList<>(entries.values());

			List<CartItem> resItems = new ArrayList<>();
			int total = 0;
			for (CartItem ci : items) {
				resItems.add(ci);
				total += ci.getPriceAtAdd() * ci.getQuantity();
			}
			return new CartResponse(resItems, total);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 조회 실패", Map.of("memberId", memberId)
			);
		}
	}

	/** 상품 추가(없으면 추가, 있으면 덮어쓰기) */
	public CartResponse addItem(Long memberId, CartItem request) {
		String key = getCartKey(memberId);
		String field = request.getProductId().toString();

		try {
			boolean isNewKey = !cartRedisTemplate.hasKey(key);
			// 기존 아이템 조회
			HashOperations<String, String, CartItem> hashOps = cartRedisTemplate.opsForHash();
			CartItem existing = hashOps.get(key, field);
			if (existing == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "장바구니에 해당 상품이 없습니다.",
					Map.of("memberId", memberId, "productId", field));
			}
			int quantity = request.getQuantity();
			if (quantity <= 0) {
				// 0 이하이면 삭제
				cartRedisTemplate.opsForHash().delete(key, field);
				ensureTtl(memberId);
				return getCart(memberId);
			}
			existing.setQuantity(quantity);
			hashOps.put(key, field, existing);

			// TTL: 최초 생성시에만 설정(7일)
			if (isNewKey) {
				cartRedisTemplate.expire(key, Duration.ofDays(7));
			}
			// TTL 보장
			ensureTtl(memberId);

			return getCart(memberId);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 저장 실패",
				Map.of("memberId", memberId, "productId", request.getProductId()));
		}
	}

	/** 상품 수정(존재 확인 후 덮어쓰기) */
	public CartResponse updateItem(Long memberId, CartItem request) {
		String key = getCartKey(memberId);
		String field = request.getProductId().toString();

		try {
			HashOperations<String, String, CartItem> hashOps = cartRedisTemplate.opsForHash();
			CartItem existing = hashOps.get(key, field);
			if (existing == null) {
				throw new BusinessException(
					ErrorCode.NOT_FOUND, "장바구니에 해당 상품이 없습니다.",
					Map.of("memberId", memberId, "productId", request.getProductId()));
			}
			if (request.getQuantity() <= 0) {
				hashOps.delete(key, field);
				// TTL 보장
				ensureTtl(memberId);
				return getCart(memberId);
			}
			existing.setQuantity(request.getQuantity());
			// 덮어쓰기(수량 변경 포함)
			hashOps.put(key, field, existing);

			// TTL 보장
			ensureTtl(memberId);

			return getCart(memberId);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 수정 실패",
				Map.of("memberId", memberId, "productId", request.getProductId()));
		}
	}

	/** 상품 삭제 */
	public CartResponse removeItem(Long memberId, Long productId) {
		String key = getCartKey(memberId);
		String field = productId.toString();
		try {
			Long removed = cartRedisTemplate.opsForHash().delete(key, field);
			if (removed == null || removed == 0) {
				throw new BusinessException(
					ErrorCode.NOT_FOUND, "장바구니에 해당 상품이 없습니다.",
					Map.of("memberId", memberId, "productId", productId));
			}
			// TTL 보장
			ensureTtl(memberId);

			return getCart(memberId);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 삭제 실패",
				Map.of("memberId", memberId, "productId", productId));
		}
	}

	/** 장바구니 비우기 */
	public void clearCart(Long memberId) {
		String key = getCartKey(memberId);
		try {
			cartRedisTemplate.delete(key);
			// TTL 보장
			ensureTtl(memberId);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 초기화 실패", Map.of("memberId", memberId));
		}
	}
}
