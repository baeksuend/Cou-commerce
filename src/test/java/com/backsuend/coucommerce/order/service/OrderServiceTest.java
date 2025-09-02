package com.backsuend.coucommerce.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.catalog.entity.Category;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderProduct;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderProductRepository;
import com.backsuend.coucommerce.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private OrderProductRepository orderProductRepository;

	@Mock
	private CartService cartService;

	@InjectMocks
	private OrderService orderService;

	private Member testMember;
	private Product testProduct1;
	private Product testProduct2;
	private CartItem cartItem1;
	private CartItem cartItem2;
	private OrderCreateRequest orderCreateRequest;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 설정
		testMember = Member.builder()
			.id(1L)
			.email("test@example.com")
			.name("테스트 사용자")
			.phone("010-1234-5678")
			.role(Role.BUYER)
			.build();

		testProduct1 = new Product();
		testProduct1.setId(1L);
		testProduct1.setName("테스트 상품 1");
		testProduct1.setPrice(10000);
		testProduct1.setStock(10);
		testProduct1.setVisible(true);
		testProduct1.setCategory(Category.DIGITAL);

		testProduct2 = new Product();
		testProduct2.setId(2L);
		testProduct2.setName("테스트 상품 2");
		testProduct2.setPrice(20000);
		testProduct2.setStock(5);
		testProduct2.setVisible(true);
		testProduct2.setCategory(Category.FASHION);

		cartItem1 = new CartItem();
		cartItem1.setProductId(1L);
		cartItem1.setQuantity(2);
		cartItem1.setPrice(10000);

		cartItem2 = new CartItem();
		cartItem2.setProductId(2L);
		cartItem2.setQuantity(1);
		cartItem2.setPrice(20000);

		orderCreateRequest = new OrderCreateRequest();
		orderCreateRequest.setConsumerName("구매자");
		orderCreateRequest.setConsumerPhone("010-1234-5678");
		orderCreateRequest.setReceiverName("수령자");
		orderCreateRequest.setReceiverRoadName("서울시 강남구 테헤란로 123");
		orderCreateRequest.setReceiverPhone("010-9876-5432");
		orderCreateRequest.setReceiverPostalCode("12345");
	}

	@Test
	@DisplayName("장바구니에서 주문 생성 성공")
	void createOrderFromCart_Success() {
		// Given
		Long memberId = 1L;
		List<CartItem> cartItems = Arrays.asList(cartItem1, cartItem2);
		CartResponse cartResponse = new CartResponse("cart:1", cartItems);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
		when(cartService.getCart(memberId)).thenReturn(cartResponse);
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct1));
		when(productRepository.findById(2L)).thenReturn(Optional.of(testProduct2));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			order.setId(1L);
			return order;
		});

		// When
		OrderResponse result = orderService.createOrderFromCart(orderCreateRequest, memberId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getOrderId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED.name());
		assertThat(result.getConsumerName()).isEqualTo("구매자");
		assertThat(result.getReceiverName()).isEqualTo("수령자");
		assertThat(result.getItems()).hasSize(2);

		// 재고 차감 확인
		assertThat(testProduct1.getStock()).isEqualTo(8); // 10 - 2
		assertThat(testProduct2.getStock()).isEqualTo(4); // 5 - 1

		// 장바구니 초기화 확인
		verify(cartService).clearCart(memberId);
	}

	@Test
	@DisplayName("존재하지 않는 회원으로 주문 생성 시 예외 발생")
	void createOrderFromCart_MemberNotFound() {
		// Given
		Long memberId = 999L;
		when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> orderService.createOrderFromCart(orderCreateRequest, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("회원을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("빈 장바구니로 주문 생성 시 예외 발생")
	void createOrderFromCart_EmptyCart() {
		// Given
		Long memberId = 1L;
		CartResponse emptyCartResponse = new CartResponse("cart:1", Arrays.asList());

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
		when(cartService.getCart(memberId)).thenReturn(emptyCartResponse);

		// When & Then
		assertThatThrownBy(() -> orderService.createOrderFromCart(orderCreateRequest, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("장바구니가 비어 있습니다.");
	}

	@Test
	@DisplayName("존재하지 않는 상품으로 주문 생성 시 예외 발생")
	void createOrderFromCart_ProductNotFound() {
		// Given
		Long memberId = 1L;
		List<CartItem> cartItems = Arrays.asList(cartItem1);
		CartResponse cartResponse = new CartResponse("cart:1", cartItems);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
		when(cartService.getCart(memberId)).thenReturn(cartResponse);
		when(productRepository.findById(1L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> orderService.createOrderFromCart(orderCreateRequest, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("상품을 찾을 수 없습니다. 상품ID: 1");
	}

	@Test
	@DisplayName("판매 중단된 상품으로 주문 생성 시 예외 발생")
	void createOrderFromCart_ProductNotVisible() {
		// Given
		Long memberId = 1L;
		testProduct1.setVisible(false);
		List<CartItem> cartItems = Arrays.asList(cartItem1);
		CartResponse cartResponse = new CartResponse("cart:1", cartItems);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
		when(cartService.getCart(memberId)).thenReturn(cartResponse);
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct1));

		// When & Then
		assertThatThrownBy(() -> orderService.createOrderFromCart(orderCreateRequest, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("판매 중단된 상품입니다: 테스트 상품 1");
	}

	@Test
	@DisplayName("가격이 변경된 상품으로 주문 생성 시 예외 발생")
	void createOrderFromCart_PriceChanged() {
		// Given
		Long memberId = 1L;
		testProduct1.setPrice(15000); // 가격 변경
		List<CartItem> cartItems = Arrays.asList(cartItem1);
		CartResponse cartResponse = new CartResponse("cart:1", cartItems);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
		when(cartService.getCart(memberId)).thenReturn(cartResponse);
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct1));

		// When & Then
		assertThatThrownBy(() -> orderService.createOrderFromCart(orderCreateRequest, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("상품 가격이 변경되었습니다. 상품: 테스트 상품 1, 최신 가격: 15000원");
	}

	@Test
	@DisplayName("재고 부족으로 주문 생성 시 예외 발생")
	void createOrderFromCart_InsufficientStock() {
		// Given
		Long memberId = 1L;
		testProduct1.setStock(1); // 재고 부족
		List<CartItem> cartItems = Arrays.asList(cartItem1);
		CartResponse cartResponse = new CartResponse("cart:1", cartItems);

		when(memberRepository.findById(memberId)).thenReturn(Optional.of(testMember));
		when(cartService.getCart(memberId)).thenReturn(cartResponse);
		when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct1));

		// When & Then
		assertThatThrownBy(() -> orderService.createOrderFromCart(orderCreateRequest, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("상품 재고가 부족합니다. 상품: 테스트 상품 1, 요청 수량: 2, 현재 재고: 1");
	}

	@Test
	@DisplayName("주문 상세 조회 성공")
	void getOrder_Success() {
		// Given
		Long orderId = 1L;
		Long memberId = 1L;
		Order order = createTestOrder();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

		// When
		OrderResponse result = orderService.getOrder(orderId, memberId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getOrderId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED.name());
	}

	@Test
	@DisplayName("존재하지 않는 주문 조회 시 예외 발생")
	void getOrder_NotFound() {
		// Given
		Long orderId = 999L;
		Long memberId = 1L;
		when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> orderService.getOrder(orderId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("주문을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("다른 사용자의 주문 조회 시 예외 발생")
	void getOrder_AccessDenied() {
		// Given
		Long orderId = 1L;
		Long memberId = 2L; // 다른 사용자
		Order order = createTestOrder();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

		// When & Then
		assertThatThrownBy(() -> orderService.getOrder(orderId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("본인의 주문만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("내 주문 목록 조회 성공")
	void getMyOrders_Success() {
		// Given
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		Order order = createTestOrder();
		Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));

		when(orderRepository.findByMemberId(memberId, pageable)).thenReturn(orderPage);

		// When
		Page<OrderResponse> result = orderService.getMyOrders(memberId, pageable);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("주문 취소 성공")
	void cancelOrder_Success() {
		// Given
		Long orderId = 1L;
		Long memberId = 1L;
		Order order = createTestOrder();
		OrderProduct orderProduct = order.getItems().get(0);
		Product product = orderProduct.getProduct();
		int originalStock = product.getStock();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

		// When
		OrderResponse result = orderService.cancelOrder(orderId, memberId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED.name());
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
		assertThat(product.getStock()).isEqualTo(originalStock + orderProduct.getQuantity());
	}

	@Test
	@DisplayName("이미 결제된 주문 취소 시 예외 발생")
	void cancelOrder_AlreadyPaid() {
		// Given
		Long orderId = 1L;
		Long memberId = 1L;
		Order order = createTestOrder();
		order.setStatus(OrderStatus.PAID);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

		// When & Then
		assertThatThrownBy(() -> orderService.cancelOrder(orderId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("주문 취소는 주문 완료 상태에서만 가능합니다. 현재 상태: PAID");
	}

	private Order createTestOrder() {
		Order order = Order.builder()
			.id(1L)
			.member(testMember)
			.consumerName("구매자")
			.consumerPhone("010-1234-5678")
			.receiverName("수령자")
			.receiverRoadName("서울시 강남구 테헤란로 123")
			.receiverPhone("010-9876-5432")
			.receiverPostalCode("12345")
			.status(OrderStatus.PLACED)
			.build();

		OrderProduct orderProduct = OrderProduct.builder()
			.id(1L)
			.order(order)
			.product(testProduct1)
			.quantity(2)
			.priceSnapshot(10000)
			.build();

		order.getItems().add(orderProduct);
		return order;
	}
}
