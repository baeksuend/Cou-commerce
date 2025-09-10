package com.backsuend.coucommerce.cart;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CartServiceTest {

    @Mock
    @Qualifier("cartRedisTemplate")
    private RedisTemplate<String, CartItem> redisTemplate;

    @Mock
    private HashOperations<String, String, CartItem> hashOps;

    @InjectMocks
    private CartService cartService;

    private static CartItem item(long id, String name, int price, int qty) {
        return CartItem.builder()
            .productId(id)
            .productName(name)
            .priceAtAdd(price)
            .quantity(qty)
            .detail("N/A")
            .build();
    }

    @BeforeEach
    void setup() {
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> cast = (HashOperations) hashOps;
        lenient().when(redisTemplate.opsForHash()).thenReturn(cast);
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {
        @Test
        @DisplayName("비어있으면 빈 리스트 + totalPrice=0")
        void emptyCart() {
            String key = "cart:1";
            given(hashOps.entries(key)).willReturn(Map.of());

            CartResponse res = cartService.getCart(1L);

            assertThat(res.getItems()).isEmpty();
            assertThat(res.getTotalPrice()).isZero();
            then(hashOps).should().entries(key);
        }

        @Test
        @DisplayName("항목 + 총액 계산")
        void returnsItemsAndTotal() {
            String key = "cart:1";
            Map<String, CartItem> data = new LinkedHashMap<>();
            data.put("1001", item(1001, "A", 1000, 2));
            data.put("1002", item(1002, "B", 2000, 1));
            given(hashOps.entries(key)).willReturn(data);

            CartResponse res = cartService.getCart(1L);
            assertThat(res.getItems()).hasSize(2);
            assertThat(res.getTotalPrice()).isEqualTo(1000*2 + 2000);
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {
        @Test
        @DisplayName("신규 추가 → put + TTL 30일")
        void addNew_put_and_ttl() {
            String key = "cart:1";
            CartItem ci = item(1001, "A", 1000, 2);
            given(hashOps.get(key, "1001")).willReturn(null);

            cartService.addItem(1L, ci);

            then(hashOps).should().put(key, "1001", ci);
            then(redisTemplate).should().expire(eq(key), eq(Duration.ofDays(30)));
        }

        @Test
        @DisplayName("기존 항목 → 수량 가산 + TTL 30일")
        void addExisting_increaseQty() {
            String key = "cart:1";
            CartItem existing = item(1001, "A", 1000, 2);
            CartItem add = item(1001, "A", 1000, 3);
            given(hashOps.get(key, "1001")).willReturn(existing);

            cartService.addItem(1L, add);

            assertThat(existing.getQuantity()).isEqualTo(5);
            then(hashOps).should().put(key, "1001", existing);
            then(redisTemplate).should().expire(eq(key), eq(Duration.ofDays(30)));
        }
    }

    @Nested
    @DisplayName("updateItem")
    class UpdateItem {
        @Test
        @DisplayName("존재하면 수량 덮어쓰기 + TTL 30일")
        void updateExisting() {
            String key = "cart:1";
            CartItem existing = item(1001, "A", 1000, 2);
            CartItem updated = item(1001, "A", 1000, 5);
            given(hashOps.get(key, "1001")).willReturn(existing);

            cartService.updateItem(1L, updated);

            then(hashOps).should().put(key, "1001", updated);
            then(redisTemplate).should().expire(eq(key), eq(Duration.ofDays(30)));
        }

        @Test
        @DisplayName("없으면 NOT_FOUND")
        void updateMissing() {
            String key = "cart:1";
            CartItem updated = item(1001, "A", 1000, 5);
            given(hashOps.get(key, "1001")).willReturn(null);

            assertThatThrownBy(() -> cartService.updateItem(1L, updated))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("remove/clear")
    class RemoveClear {
        @Test
        @DisplayName("productId로 삭제 OK")
        void removeById_ok() {
            String key = "cart:1";
            given(redisTemplate.opsForHash().delete(key, "1001")).willReturn(1L);

            assertThatCode(() -> cartService.removeItem(1L, 1001L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("키 삭제 OK")
        void clear_ok() {
            String key = "cart:1";
            assertThatCode(() -> cartService.clearCart(1L)).doesNotThrowAnyException();
            then(redisTemplate).should().delete(key);
        }
    }
}

