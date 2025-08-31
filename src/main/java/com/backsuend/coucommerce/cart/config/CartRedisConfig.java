package com.backsuend.coucommerce.cart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.backsuend.coucommerce.cart.dto.CartItem;

/**
 * @author rua
 */
@Configuration
public class CartRedisConfig {

	@Bean(name = "cartRedisTemplate")
	public RedisTemplate<String, CartItem> cartRedisTemplate(RedisConnectionFactory cf) {
		RedisTemplate<String, CartItem> template = new RedisTemplate<>();
		template.setConnectionFactory(cf);

		template.setKeySerializer(new StringRedisSerializer());          // "cart:1" ← 사람이 읽을 수 있는 키
		template.setHashKeySerializer(new StringRedisSerializer());      // "1001"   ← 사람이 읽을 수 있는 필드

		template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(CartItem.class)); // 값 = 순수 JSON

		// @Bean 환경에선 컨테이너가 afterPropertiesSet()을 자동 호출하므로 보통 생략
		// template.afterPropertiesSet(); // (선택) 수동 생성 시엔 필요

		return template;
	}
}
