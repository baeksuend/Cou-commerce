package com.backsuend.coucommerce.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CacheIntegrationTests {

	@Autowired
	private TestCacheService testCacheService;

	@Test
	@DisplayName("Record 캐시 직렬화/역직렬화 성공 테스트")
	void recordCacheSuccessTest() {
		Long id = 1L;

		System.out.println("--- First call (populating cache) ---");
		UserRecord user1 = testCacheService.getUser(id);
		System.out.println("Received: " + user1);

		System.out.println("\n--- Second call (should hit cache) ---");
		UserRecord user2 = testCacheService.getUser(id);
		System.out.println("Received from cache: " + user2);

		// 캐시된 객체가 원본 객체와 동일한지 확인
		assertThat(user2).isEqualTo(user1);
		System.out.println("\nTest successful!");
	}
}
