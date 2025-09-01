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
 * */
@Service
@RequiredArgsConstructor
public class CartService {

	@Qualifier("cartRedisTemplate")
	private final RedisTemplate<String, CartItem> cartRedisTemplate;

	private String getCartKey(Long memberId) {
		return "cart:" + memberId;
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

			return new CartResponse(key, items);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 조회 실패", Map.of("memberId", memberId)
			);
		}
	}

	/** 상품 추가(없으면 추가, 있으면 덮어쓰기) */
	public CartResponse addItem(Long memberId, CartItem item) {
		String key = getCartKey(memberId);
		String field = item.getProductId().toString();
		try {
			boolean isNewKey = !cartRedisTemplate.hasKey(key);
			cartRedisTemplate.opsForHash().put(key, field, item);

			// TTL: 최초 생성시에만 설정(7일)
			if (isNewKey) {
				cartRedisTemplate.expire(key, Duration.ofDays(7));
			}
			// 접근 시마다 TTL 갱신하고 싶으면 여기 추가 : cartRedisTemplate.expire(key, Duration.ofDays(7));

			return getCart(memberId);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 저장 실패",
				Map.of("memberId", memberId, "productId", item.getProductId()));
		}
	}

	/** 상품 수정(존재 확인 후 덮어쓰기) */
	public CartResponse updateItem(Long memberId, CartItem item) {
		String key = getCartKey(memberId);
		String field = item.getProductId().toString();

		try {
			HashOperations<String, String, CartItem> hashOps = cartRedisTemplate.opsForHash();
			CartItem existing = hashOps.get(key, field);
			if (existing == null) {
				throw new BusinessException(
					ErrorCode.NOT_FOUND, "장바구니에 해당 상품이 없습니다.",
					Map.of("memberId", memberId, "productId", item.getProductId()));
			}
			hashOps.put(key, field, item);
			// 필요시 TTL 갱신 정책 선택
			cartRedisTemplate.expire(key, Duration.ofDays(7));
			return getCart(memberId);
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 수정 실패",
				Map.of("memberId", memberId, "productId", item.getProductId()));
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
		} catch (DataAccessException dae) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR, "장바구니 초기화 실패", Map.of("memberId", memberId));
		}
	}
}
