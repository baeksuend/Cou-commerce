package com.backsuend.coucommerce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class CouCommerceApplication {
	private static final Logger log = LoggerFactory.getLogger(CouCommerceApplication.class);

	public static void main(String[] args) {
		log.info("CouCommerce 애플리케이션 시작");
		SpringApplication.run(CouCommerceApplication.class, args);
		log.info("CouCommerce 애플리케이션 실행 완료");
	}

}
