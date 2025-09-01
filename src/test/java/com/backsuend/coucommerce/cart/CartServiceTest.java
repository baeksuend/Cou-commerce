package com.backsuend.coucommerce.cart;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

/**
 * @author rua
 */

/**
 * ToDo
 *  1. setUp : lenient().when(cartRedisTemplate.opsForHash()).thenReturn(cast); 은 현재 테스트 코드에서 사용되는 쪽에만 적용하게 이동 시켜야함.
 *  2.
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CartServiceTest {

	@Mock
	@Qualifier("cartRedisTemplate")
	private RedisTemplate<String, CartItem> cartRedisTemplate;

	@Mock
	private HashOperations<String, String, CartItem> hashOps;

	@InjectMocks
	private CartService sut; // System Under Test

	private static CartItem item(long id, String name, int price, int qty, String detail) {
		return CartItem.builder()
			.productId(id)
			.name(name)
			.price(price)
			.quantity(qty)
			.detail(detail)
			.build();
	}

	@BeforeEach
	void setUp() {
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> cast = (HashOperations)hashOps;
		lenient().when(cartRedisTemplate.opsForHash()).thenReturn(cast); // ← cast 사용!	}
	}

	@Nested
	@DisplayName("getCart")
	class GetCart {

		@Test
		@DisplayName("장바구니가 비어있으면 빈 리스트를 반환한다")
		void emptyCart() {
			// given
			String key = "cart:1";
			when(hashOps.entries(key)).thenReturn(Map.of());

			// when
			CartResponse res = sut.getCart(1L);

			// then
			assertEquals(key, res.getCartId());
			assertNotNull(res.getItems());
			assertTrue(res.getItems().isEmpty());
			verify(hashOps).entries(key);
		}

		@Test
		@DisplayName("장바구니의 모든 항목을 반환한다")
		void returnsAllItems() {
			// given
			String key = "cart:1";
			Map<String, CartItem> data = new LinkedHashMap<>();
			data.put("1001", item(1001, "나이키", 195000, 5, "BLUE-XL"));
			data.put("1002", item(1002, "아디다스", 99000, 1, "BLACK-M"));

			when(hashOps.entries(key)).thenReturn(data);

			// when
			CartResponse res = sut.getCart(1L);

			// then
			assertEquals(key, res.getCartId());
			assertEquals(2, res.getItems().size());
			assertEquals(new ArrayList<>(data.values()), res.getItems());
			verify(hashOps).entries(key);
		}

		@Test
		@DisplayName("Redis 접근에 실패하면 BusinessException(INTERNAL_ERROR)을 던진다")
		void dataAccessError() {
			// given
			String key = "cart:1";
			when(hashOps.entries(key)).thenThrow(new DataAccessResourceFailureException("boom"));

			// when / then
			BusinessException ex = assertThrows(BusinessException.class, () -> sut.getCart(1L));
			assertEquals(ErrorCode.INTERNAL_ERROR, ex.errorCode());
			verify(hashOps).entries(key);
		}
	}

	@Nested
	@DisplayName("addItem")
	class AddItem {

		@Test
		@DisplayName("새 키면 put 후 TTL(7일)을 설정한다")
		void newKey_setsTTL() {
			// given
			String key = "cart:1";
			CartItem ci = item(1001, "나이키", 195000, 5, "BLUE-XL");
			when(cartRedisTemplate.hasKey(key)).thenReturn(false);

			// when
			sut.addItem(1L, ci);

			// then
			verify(cartRedisTemplate).hasKey(key);
			verify(hashOps).put(key, "1001", ci);
			verify(cartRedisTemplate).expire(key, Duration.ofDays(7));
		}

		@Test
		@DisplayName("기존 키면 TTL은 설정하지 않는다")
		void existingKey_noTTL() {
			// given
			String key = "cart:1";
			CartItem ci = item(1001, "나이키", 195000, 5, "BLUE-XL");
			when(cartRedisTemplate.hasKey(key)).thenReturn(true);

			// when
			sut.addItem(1L, ci);

			// then
			verify(cartRedisTemplate).hasKey(key);
			verify(hashOps).put(key, "1001", ci);
			verify(cartRedisTemplate, never()).expire(eq(key), any());
		}

		@Test
		@DisplayName("Redis 에러 시 BusinessException(INTERNAL_ERROR)")
		void dataAccessError() {
			// given
			String key = "cart:1";
			CartItem ci = item(1001, "나이키", 195000, 5, "BLUE-XL");
			when(cartRedisTemplate.hasKey(key)).thenReturn(false);
			doThrow(new DataAccessResourceFailureException("boom"))
				.when(hashOps).put(key, "1001", ci);

			// when / then
			BusinessException ex = assertThrows(BusinessException.class, () -> sut.addItem(1L, ci));
			assertEquals(ErrorCode.INTERNAL_ERROR, ex.errorCode());
			// expire는 put 전에 hasKey를 보고 분기하므로 호출 안 되었을 수도 있음
		}
	}

	@Nested
	@DisplayName("updateItem")
	class UpdateItem {

		@Test
		@DisplayName("존재하면 덮어쓰고 TTL을 갱신한다")
		void updateExisting() {
			// given
			String key = "cart:1";
			CartItem existing = item(1001, "나이키", 195000, 5, "BLUE-XL");
			CartItem updated = item(1001, "나이키", 185000, 3, "BLACK-L");

			when(hashOps.get(key, "1001")).thenReturn(existing);

			// when
			sut.updateItem(1L, updated);

			// then
			verify(hashOps).get(key, "1001");
			verify(hashOps).put(key, "1001", updated);
			verify(cartRedisTemplate).expire(key, Duration.ofDays(7)); // 현재 구현은 무조건 갱신
		}

		@Test
		@DisplayName("존재하지 않으면 BusinessException(NOT_FOUND)")
		void updateMissing_throws() {
			// given
			String key = "cart:1";
			CartItem updated = item(1001, "나이키", 185000, 3, "BLACK-L");
			when(hashOps.get(key, "1001")).thenReturn(null);

			// when / then
			BusinessException ex = assertThrows(BusinessException.class, () -> sut.updateItem(1L, updated));
			assertEquals(ErrorCode.NOT_FOUND, ex.errorCode());
			verify(hashOps).get(key, "1001");
			verify(hashOps, never()).put(any(), any(), any());
		}

		@Test
		@DisplayName("Redis 에러 시 BusinessException(INTERNAL_ERROR)")
		void dataAccessError() {
			String key = "cart:1";
			CartItem updated = item(1001, "나이키", 185000, 3, "BLACK-L");
			when(hashOps.get(key, "1001")).thenThrow(new DataAccessResourceFailureException("boom"));

			BusinessException ex = assertThrows(BusinessException.class, () -> sut.updateItem(1L, updated));
			assertEquals(ErrorCode.INTERNAL_ERROR, ex.errorCode());
		}
	}

	@Nested
	@DisplayName("removeItem")
	class RemoveItem {

		@Test
		@DisplayName("삭제 성공 시 예외 없이 끝난다")
		void deleteOk() {
			// given
			String key = "cart:1";
			when(cartRedisTemplate.opsForHash().delete(key, "1001")).thenReturn(1L);

			// when / then
			assertDoesNotThrow(() -> sut.removeItem(1L, 1001L));

			verify(cartRedisTemplate.opsForHash()).delete(key, "1001");
		}

		@Test
		@DisplayName("삭제 대상이 없으면 BusinessException(NOT_FOUND)")
		void deleteMissing_throws() {
			// given
			String key = "cart:1";
			when(cartRedisTemplate.opsForHash().delete(key, "1001")).thenReturn(0L);

			// when / then
			BusinessException ex = assertThrows(BusinessException.class, () -> sut.removeItem(1L, 1001L));
			assertEquals(ErrorCode.NOT_FOUND, ex.errorCode());
		}

		@Test
		@DisplayName("Redis 에러 시 BusinessException(INTERNAL_ERROR)")
		void dataAccessError() {
			// given
			String key = "cart:1";
			when(cartRedisTemplate.opsForHash().delete(key, "1001"))
				.thenThrow(new DataAccessResourceFailureException("boom"));

			// when / then
			BusinessException ex = assertThrows(BusinessException.class, () -> sut.removeItem(1L, 1001L));
			assertEquals(ErrorCode.INTERNAL_ERROR, ex.errorCode());
		}
	}

	@Nested
	@DisplayName("clearCart")
	class ClearCart {

		@Test
		@DisplayName("키를 삭제한다")
		void clear() {
			// given
			String key = "cart:1";

			// when
			sut.clearCart(1L);

			// then
			verify(cartRedisTemplate).delete(key);
		}

		@Test
		@DisplayName("Redis 에러 시 BusinessException(INTERNAL_ERROR)")
		void dataAccessError() {
			// given
			String key = "cart:1";
			doThrow(new DataAccessResourceFailureException("boom"))
				.when(cartRedisTemplate).delete(key);

			// when / then
			BusinessException ex = assertThrows(BusinessException.class, () -> sut.clearCart(1L));
			assertEquals(ErrorCode.INTERNAL_ERROR, ex.errorCode());
		}
	}
}
