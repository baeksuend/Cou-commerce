package com.backsuend.coucommerce.cart;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.service.CartService;

/**
 * @author rua
 */

@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class CartServiceTtlTest {

	@Autowired
	private CartService cartService;
	@Autowired
	private RedisTemplate<String, CartItem> cartRedisTemplate;

	private String key(Long memberId) {
		return "cart:" + memberId;
	}

	@AfterEach
	void tearDown() {
		if (cartRedisTemplate.getConnectionFactory() != null) {
			var conn = cartRedisTemplate.getConnectionFactory().getConnection();
			try {
				conn.serverCommands().flushAll();
			} finally {
				conn.close();
			}
		}
	}

	@Test
	@DisplayName("아이템추가_시_TTL이_30일로_설정된다")
	void addItem_setsTtlTo30Days_onWrite() {
		Long memberId = 4242L;
		CartItem item = CartItem.builder()
			.productId(1001L)
			.name("테스트상품")
			.price(15000)
			.quantity(1)
			.detail("옵션A")
			.build();

		cartService.addItem(memberId, item);

		Long ttlSec = cartRedisTemplate.getExpire(key(memberId), TimeUnit.SECONDS);
		assertThat(ttlSec).isNotNull();
		// 여유 범위를 둠 (>= 25일 && <= 30일)
		assertThat(ttlSec).isBetween(25L * 24 * 3600, 30L * 24 * 3600);
	}

	@Test
	@DisplayName("아이템수정_시_TTL이_갱신된다")
	void updateItem_refreshesTtl_onWrite() {
		Long memberId = 4243L;
		CartItem item = CartItem.builder()
			.productId(2002L)
			.name("테스트상품2")
			.price(20000)
			.quantity(1)
			.detail("옵션B")
			.build();

		cartService.addItem(memberId, item);
		Long ttl1 = cartRedisTemplate.getExpire(key(memberId), TimeUnit.SECONDS);
		assertThat(ttl1).isNotNull();

		// 수량 증가 → TTL 갱신 기대
		CartItem updated = CartItem.builder()
			.productId(2002L)
			.name("테스트상품2")
			.price(20000)
			.quantity(3)
			.detail("옵션B")
			.build();
		cartService.updateItem(memberId, updated);

		Long ttl2 = cartRedisTemplate.getExpire(key(memberId), TimeUnit.SECONDS);
		assertThat(ttl2).isNotNull();
		assertThat(ttl2).isBetween(25L * 24 * 3600, 30L * 24 * 3600);
	}

	@Test
	@DisplayName("아이템을_삭제해도_장바구니_TTL이_30일로_보장된다")
	void removeItem_keepsOrResetsTtl_onWrite() {
		Long memberId = 4244L;
		CartItem it1 = CartItem.builder().productId(3001L).name("A").price(1000).quantity(1).detail("d").build();
		CartItem it2 = CartItem.builder().productId(3002L).name("B").price(2000).quantity(2).detail("d").build();

		cartService.addItem(memberId, it1);
		cartService.addItem(memberId, it2);

		// 하나 삭제 → 키가 남아있는 한 TTL은 갱신되어야 함
		cartService.removeItem(memberId, 3001L);

		Long ttlSec = cartRedisTemplate.getExpire(key(memberId), TimeUnit.SECONDS);
		assertThat(ttlSec).isNotNull();
		assertThat(ttlSec).isBetween(25L * 24 * 3600, 30L * 24 * 3600);
	}
}
