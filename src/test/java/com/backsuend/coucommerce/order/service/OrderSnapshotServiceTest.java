package com.backsuend.coucommerce.order.service;

import static org.assertj.core.api.Assertions.*;
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
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderProduct;

/**
 * @author rua
 */
@ExtendWith(MockitoExtension.class)
class OrderSnapshotServiceTest {

	@Mock
	ProductRepository productRepository;

	@InjectMocks
	OrderSnapshotService service;

	@Test
	void cartItem을_OrderProduct로_스냅샷화() {
		// given
		Order order = new Order();
		CartItem c = CartItem.builder().productId(42L).name("상품").price(7000).quantity(3).detail("옵션").build();

		Product p = Product.builder().id(42L).name("상품").price(7000).stock(100).build();
		when(productRepository.findById(42L)).thenReturn(Optional.of(p));

		// when
		List<OrderProduct> list = service.toOrderProducts(order, List.of(c));

		// then
		assertThat(list).hasSize(1);
		OrderProduct op = list.get(0);
		assertThat(op.getOrder()).isSameAs(order);
		assertThat(op.getProduct()).isSameAs(p);
		assertThat(op.getQuantity()).isEqualTo(3);
		assertThat(op.getPriceSnapshot()).isEqualTo(7000);
	}
}
