package com.backsuend.coucommerce.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author rua
 */
@Configuration
public class RedisConfig {
	@Bean
	public RedisTemplate<String, CartItem> cartRedisTemplate(RedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper) {
		RedisTemplate<String, CartItem> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// key, hashKey → String
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());

		// value, hashValue → CartItem JSON
		Jackson2JsonRedisSerializer<CartItem> serializer = new Jackson2JsonRedisSerializer<>(CartItem.class);
		serializer.setObjectMapper(objectMapper);
		template.setValueSerializer(serializer);
		template.setHashValueSerializer(serializer);

		template.afterPropertiesSet();
		return template;
	}
}
