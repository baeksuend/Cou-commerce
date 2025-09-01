package com.backsuend.coucommerce.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TestCacheService {

	@Cacheable(value = "test-users", key = "#id")
	public UserRecord getUser(Long id) {
		System.out.println("Fetching user from service for id: " + id);
		return new UserRecord(id, "User-" + id);
	}
}
