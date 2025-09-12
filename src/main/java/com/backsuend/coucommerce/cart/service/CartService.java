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
import lombok.extern.slf4j.Slf4j;
import com.backsuend.coucommerce.cart.logging.CartLogContext;

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
@Slf4j
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
        CartLogContext.setCartContext(memberId, null, 0);
        String key = getCartKey(memberId);
        try {
            HashOperations<String, String, CartItem> hashOps = cartRedisTemplate.opsForHash();
            Map<String, CartItem> entries = hashOps.entries(key);

            List<CartItem> items = new ArrayList<>(entries.values());

            List<CartItem> resItems = new ArrayList<>();
            int total = 0;
            for (CartItem ci : items) {
                resItems.add(ci);
                total += ci.getPriceAtAdd() * ci.getQuantity();
            }
            CartResponse result = new CartResponse(resItems, total);
            log.info("cart.get: total_items={}, total_price={}", resItems.size(), total);
            return result;
        } catch (DataAccessException dae) {
            throw new BusinessException(
                ErrorCode.INTERNAL_ERROR, "장바구니 조회 실패", Map.of("memberId", memberId)
            );
        } finally {
            CartLogContext.clear();
        }
    }

	/** 상품 추가(없으면 신규 추가, 있으면 수량 가산) */
    public CartResponse addItem(Long memberId, CartItem request) {
        String key = getCartKey(memberId);
        String field = request.getProductId().toString();
        CartLogContext.setCartContext(memberId, request.getProductId(), request.getQuantity());
        String resultTag = "added";
        try {
            HashOperations<String, String, CartItem> hashOps = cartRedisTemplate.opsForHash();
            CartItem existing = hashOps.get(key, field);
            int quantity = request.getQuantity();
            if (existing == null) {
                if (quantity <= 0) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "수량은 1 이상이어야 합니다.",
                        Map.of("memberId", memberId, "productId", field, "quantity", quantity));
                }
                hashOps.put(key, field, request);
            } else {
                int newQty = existing.getQuantity() + quantity;
                if (newQty <= 0) {
                    cartRedisTemplate.opsForHash().delete(key, field);
                    resultTag = "removed_by_nonpositive";
                } else {
                    existing.setQuantity(newQty);
                    hashOps.put(key, field, existing);
                }
            }
            ensureTtl(memberId);

            CartResponse result = getCart(memberId);
            log.info("cart.add: result={}, new_total_items={}, new_total_price={}",
                resultTag, result.getItems() == null ? 0 : result.getItems().size(), result.getTotalPrice());
            return result;
        } catch (DataAccessException dae) {
            throw new BusinessException(
                ErrorCode.INTERNAL_ERROR, "장바구니 저장 실패",
                Map.of("memberId", memberId, "productId", request.getProductId()));
        } finally {
            CartLogContext.clear();
        }
    }

	/** 상품 수정(존재 확인 후 덮어쓰기) */
    public CartResponse updateItem(Long memberId, CartItem request) {
        String key = getCartKey(memberId);
        String field = request.getProductId().toString();
        CartLogContext.setCartContext(memberId, request.getProductId(), request.getQuantity());
        String resultTag = "updated";
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
                ensureTtl(memberId);
                resultTag = "deleted_by_zero";
                CartResponse res = getCart(memberId);
                log.info("cart.update: result={}, new_total_items={}, new_total_price={}",
                    resultTag, res.getItems() == null ? 0 : res.getItems().size(), res.getTotalPrice());
                return res;
            }
            hashOps.put(key, field, request);
            ensureTtl(memberId);
            CartResponse res = getCart(memberId);
            log.info("cart.update: result={}, new_total_items={}, new_total_price={}",
                resultTag, res.getItems() == null ? 0 : res.getItems().size(), res.getTotalPrice());
            return res;
        } catch (DataAccessException dae) {
            throw new BusinessException(
                ErrorCode.INTERNAL_ERROR, "장바구니 수정 실패",
                Map.of("memberId", memberId, "productId", request.getProductId()));
        } finally {
            CartLogContext.clear();
        }
    }

	/** 상품 삭제 (productId 경로 변수 기반) */
    public CartResponse removeItem(Long memberId, Long productId) {
        String key = getCartKey(memberId);
        String field = productId.toString();
        CartLogContext.setCartContext(memberId, productId, 0);
        try {
            Long removed = cartRedisTemplate.opsForHash().delete(key, field);
            if (removed == null || removed == 0) {
                throw new BusinessException(
                    ErrorCode.NOT_FOUND, "장바구니에 해당 상품이 없습니다.",
                    Map.of("memberId", memberId, "productId", field));
            }
            ensureTtl(memberId);
            CartResponse res = getCart(memberId);
            log.info("cart.remove: result=removed, new_total_items={}, new_total_price={}",
                res.getItems() == null ? 0 : res.getItems().size(), res.getTotalPrice());
            return res;
        } catch (DataAccessException dae) {
            throw new BusinessException(
                ErrorCode.INTERNAL_ERROR, "장바구니 삭제 실패",
                Map.of("memberId", memberId, "productId", field));
        } finally {
            CartLogContext.clear();
        }
    }

	/** 장바구니 비우기 */
    public void clearCart(Long memberId) {
        String key = getCartKey(memberId);
        CartLogContext.setCartContext(memberId, null, 0);
        try {
            cartRedisTemplate.delete(key);
            ensureTtl(memberId);
            log.info("cart.clear: result=cleared");
        } catch (DataAccessException dae) {
            throw new BusinessException(
                ErrorCode.INTERNAL_ERROR, "장바구니 초기화 실패", Map.of("memberId", memberId));
        } finally {
            CartLogContext.clear();
        }
    }
}
