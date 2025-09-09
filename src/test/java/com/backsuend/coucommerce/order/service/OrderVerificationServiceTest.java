package com.backsuend.coucommerce.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.order.verification.OrderVerificationService;

/**
 * @author rua
 */

@ExtendWith(MockitoExtension.class)
class OrderVerificationServiceTest {

	@Mock
	ProductRepository productRepository;

	@InjectMocks
	OrderVerificationService service;

	@Test
	void 재고부족이면_CONFLICT() {
		CartItem c = CartItem.builder()
			.productId(101L).name("상품").price(1000).quantity(5).detail("옵션")
			.build();
		Product p = Product.builder().id(101L).name("상품").price(1000).stock(3).build();
		when(productRepository.findById(101L)).thenReturn(Optional.of(p));

		assertThatThrownBy(() -> service.verify(List.of(c)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void 가격변동이면_CONFLICT() {
		CartItem c = CartItem.builder()
			.productId(101L).name("상품").price(1000).quantity(1).detail("옵션")
			.build();
		Product p = Product.builder().id(101L).name("상품").price(1200).stock(10).build();
		when(productRepository.findById(101L)).thenReturn(Optional.of(p));

		assertThatThrownBy(() -> service.verify(List.of(c)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void 상품없으면_NOT_FOUND() {
		CartItem c = CartItem.builder()
			.productId(999L).name("X").price(1000).quantity(1).detail("옵션")
			.build();
		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.verify(List.of(c)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}
}
