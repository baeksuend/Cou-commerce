package com.backsuend.coucommerce.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderProduct;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderRepository;
import com.backsuend.coucommerce.order.verification.OrderVerificationService;

/**
 * @author rua
 */
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CartService cartService;

	@Mock
	private OrderVerificationService orderVerificationService;

	@Mock
	private OrderSnapshotService orderSnapshotService;

	@InjectMocks
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		orderService = new OrderService(orderRepository, productRepository, memberRepository, cartService,
			orderVerificationService, orderSnapshotService);
	}

	private Member createTestMember(Long id) {
		return Member.builder()
			.id(id)
			.email("test@example.com" + id)
			.name("테스트 사용자" + id)
			.build();
	}

	private Product createTestProduct(Long id, Member member, String name, int price, String detail, Category category,
		int stock, boolean visible) {
		return Product.builder()
			.id(id)
			.member(member)
			.name(name)
			.price(price)
			.stock(stock)
			.detail(detail)
			.category(category)
			.visible(visible)
			.build();
	}

	private CartItem createTestCartItem(Long productId, String name, int price, int quantity) {
		return CartItem.builder()
			.productId(productId)
			.name(name)
			.price(price)
			.quantity(quantity)
			.detail("색상: 블랙")
			.build();
	}

	private OrderCreateRequest createTestOrderCreateRequest() {
		return OrderCreateRequest.builder()
			.consumerName("홍길동")
			.consumerPhone("010-1234-5678")
			.receiverName("홍길동")
			.receiverPhone("010-1234-5678")
			.receiverRoadName("서울시 강남구 테헤란로 123")
			.receiverPostalCode("06292")
			.build();
	}

	private Order createTestOrder(Member member, Product product) {
		Order order = Order.builder()
			.member(member)
			.consumerName("홍길동")
			.consumerPhone("010-1234-5678")
			.receiverName("홍길동")
			.receiverPhone("010-1234-5678")
			.receiverRoadName("서울시 강남구 테헤란로 123")
			.receiverPostalCode("06292")
			.status(OrderStatus.PLACED)
			.build();

		order.setId(1L);

		OrderProduct orderProduct = OrderProduct.builder()
			.product(product)
			.quantity(2)
			.priceSnapshot(10000)
			.build();

		order.addItem(orderProduct);

		return order;
	}

	// ===== 테스트 헬퍼 메서드들 =====

	@Nested
	@DisplayName("주문 생성 테스트")
	class CreateOrderTest {

		@Test
		@DisplayName("정상적인 주문 생성 - 성공")
		void createOrderFromCart_Success() {
			// given
			Member testMember1 = createTestMember(1L);
			Member testMember2 = createTestMember(2L);

			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Product testProduct2 = createTestProduct(2L, testMember2, "테스트 상품 2", 20000, "Red", Category.FOOD, 5, true);

			CartItem testCartItem1 = createTestCartItem(1L, "테스트 상품 1", 10000, 2);
			CartItem testCartItem2 = createTestCartItem(2L, "테스트 상품 2", 20000, 1);

			CartResponse testCartResponse = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList(testCartItem1, testCartItem2))
				.build();

			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();

			// Mock 설정 - 명확하게 설정
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember1));
			given(memberRepository.findById(2L)).willReturn(Optional.of(testMember2));
			given(cartService.getCart(1L)).willReturn(testCartResponse);
			given(productRepository.findById(testProduct1.getId())).willReturn(Optional.of(testProduct1));
			given(productRepository.findById(testProduct2.getId())).willReturn(Optional.of(testProduct2));
			given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
				Order order = invocation.getArgument(0);
				order.setId(1L);
				return order;
			});

			// when
			OrderResponse result = orderService.createOrderFromCart(testOrderCreateRequest, 1L);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getOrderId()).isEqualTo(1L);
			assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED.name());
			assertThat(result.getConsumerName()).isEqualTo("홍길동");
			assertThat(result.getItems()).hasSize(2);

			// 재고 차감 확인
			assertThat(testProduct1.getStock()).isEqualTo(8); // 10 - 2
			assertThat(testProduct2.getStock()).isEqualTo(4); // 5 - 1

			// 장바구니 초기화 확인
			verify(cartService).clearCart(1L);
		}
/*

		@Test
		@Transactional
		@DisplayName("판매 중단된 상품으로 주문 생성 - 실패")
		void createOrderFromCart_ProductNotVisible_Fail() {
			// given
			Member testMember = createTestMember(1L);
			Product invisibleProduct = createTestProduct(1L, testMember, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			OrderCreateRequest req = createTestOrderCreateRequest();
			CartItem cartItem = createTestCartItem(1L, "테스트 상품 1", 10000, 2);
			CartResponse cartResponse = CartResponse.builder().cartId("cart:1").items(Arrays.asList(cartItem)).build();

			given(memberRepository.findById(testMember.getId())).willReturn(Optional.of(testMember));
			given(cartService.getCart(cartItem.getProductId())).willReturn(cartResponse);
			given(productRepository.findById(invisibleProduct.getId())).willReturn(Optional.of(invisibleProduct));

			// 1) 스텁이 제대로 적용됐는지 확인
			System.out.println(
				">>> cartService.getCart(1L) = " + cartService.getCart(testMember.getId())); // CartResponse가 나와야 함
			System.out.println(
				">>> productRepository.findById(1L) present? = " + productRepository.findById(invisibleProduct.getId())
					.isPresent());

			//****************************************************************************************************
			memberRepository.save(testMember); // 반드시 save 필요
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();
			Order order = Order.builder()
				.member(testMember)
				.consumerName(testOrderCreateRequest.getConsumerName())
				.consumerPhone(testOrderCreateRequest.getConsumerPhone())
				.receiverName(testOrderCreateRequest.getReceiverName())
				.receiverRoadName(testOrderCreateRequest.getReceiverRoadName())
				.receiverPhone(testOrderCreateRequest.getReceiverPhone())
				.receiverPostalCode(testOrderCreateRequest.getReceiverPostalCode())
				.build();

			Order savedOrder = orderRepository.saveAndFlush(order);
			assertNotNull(savedOrder.getId());

			System.out.println("	test order.getId=" + savedOrder.getId());
			//****************************************************************************************************

			// 2) 서비스 실행 후 실제 productRepository에 어떤 인자가 들어갔는지 캡처
			ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
			try {
				orderService.createOrderFromCart(req, testMember.getId());
			} catch (BusinessException e) {
				// expected for this test
			}

			verify(productRepository, atLeastOnce()).findById(captor.capture());
			System.out.println(">>> productRepository.findById called with: " + captor.getAllValues());

			// when & then — 메시지 일부분으로 검사 (더 유연)
			assertThatThrownBy(() -> orderService.createOrderFromCart(req, testMember.getId()))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("판매 중단된 상품");

			// 호출 검증: 실제로 상품 조회가 일어났는지 확인
			verify(productRepository).findById(invisibleProduct.getId());
			verify(cartService, never()).clearCart(anyLong()); // 실패 케이스라면 clearCart가 호출되면 안됨
		}

		@Test
		@DisplayName("가격이 변경된 상품으로 주문 생성 - 실패")
		void createOrderFromCart_PriceChanged_Fail() {
			// given
			Member testMember = createTestMember(1L);
			Product priceChangedProduct = createTestProduct(1L, testMember, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();

			CartItem testCartItem1 = createTestCartItem(1L, "테스트 상품 1", 10000, 2); // 장바구니 가격과 다름
			CartResponse testCartResponse = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList(testCartItem1))
				.build();

			// Mock 설정 - 이 테스트에서만 사용
			given(memberRepository.findById(testMember.getId())).willReturn(Optional.of(testMember));
			given(cartService.getCart(1L)).willReturn(testCartResponse);
			given(productRepository.findById(priceChangedProduct.getId())).willReturn(Optional.of(priceChangedProduct));

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(testOrderCreateRequest, testMember.getId()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("상품 가격이 변경되었습니다. 상품: 테스트 상품 1, 최신 가격: 15000원");
		}
*/

		@Test
		@DisplayName("재고 부족으로 주문 생성 - 실패")
		void createOrderFromCart_InsufficientStock_Fail() {
			// given
			Member testMember1 = createTestMember(1L);
			Product lowStockProduct = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 1,
				true);
			OrderCreateRequest req = createTestOrderCreateRequest();

			CartItem cartItem = createTestCartItem(1L, "테스트 상품 1", 10000, 2); // 요청 수량이 재고보다 많음
			CartResponse cartResponse = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList(cartItem))
				.build();

			// Mock 설정 (명시적 매처 사용)
			given(memberRepository.findById(eq(1L))).willReturn(Optional.of(testMember1));
			given(cartService.getCart(eq(1L))).willReturn(cartResponse);
			given(productRepository.findById(eq(lowStockProduct.getId()))).willReturn(Optional.of(lowStockProduct));

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(req, 1L))
				.isInstanceOf(BusinessException.class)
				// 핵심 메시지는 포함 여부로 검사 (포맷 민감도 제거)
				.hasMessageContaining("상품 재고가 부족합니다")
				// 추가로 상품명·수량·재고가 포함되었는지도 확인 (선택적 강도 높임)
				.hasMessageContaining("테스트 상품 1")
				.hasMessageContaining("요청 수량")
				.hasMessageContaining("현재 재고");

			// 호출 검증: 상품 조회는 했고, 실패 시 장바구니 초기화나 주문 저장은 일어나지 않아야 함
			verify(productRepository).findById(lowStockProduct.getId());
			verify(cartService, never()).clearCart(anyLong());
			verify(orderRepository, never()).save(any(Order.class));
		}

		@Test
		@DisplayName("존재하지 않는 상품으로 주문 생성 - 실패")
		void createOrderFromCart_ProductNotFound_Fail() {
			// given
			Member testMember = createTestMember(1L);
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();

			CartItem testCartItem1 = createTestCartItem(1L, "테스트 상품 1", 10000, 2);
			CartResponse testCartResponse = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList(testCartItem1))
				.build();

			// Mock 설정 - 상품을 찾을 수 없도록 설정
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(cartService.getCart(1L)).willReturn(testCartResponse);
			given(productRepository.findById(1L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(testOrderCreateRequest, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("상품을 찾을 수 없습니다. 상품ID: 1");
		}

		@Test
		@DisplayName("빈 장바구니로 주문 생성 - 실패")
		void createOrderFromCart_EmptyCart_Fail() {
			// given
			Member testMember = createTestMember(1L);
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();

			CartResponse emptyCart = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList())
				.build();

			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(cartService.getCart(1L)).willReturn(emptyCart);

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(testOrderCreateRequest, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("장바구니가 비어 있습니다.");
		}

		@Test
		@DisplayName("존재하지 않는 회원으로 주문 생성 - 실패")
		void createOrderFromCart_MemberNotFound_Fail() {
			// given
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(testOrderCreateRequest, 999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("회원을 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("수량이 0 이하인 상품으로 주문 생성 - 실패")
		void createOrderFromCart_InvalidQuantity_Fail() {
			// given
			Member testMember = createTestMember(1L);
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();

			CartItem invalidCartItem = createTestCartItem(1L, "테스트 상품 1", 10000, 0); // 잘못된 수량
			CartResponse invalidCart = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList(invalidCartItem))
				.build();

			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(cartService.getCart(1L)).willReturn(invalidCart);

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(testOrderCreateRequest, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("수량은 1 이상이어야 합니다. 상품ID: 1");
		}

		@Test
		@DisplayName("가격이 음수인 상품으로 주문 생성 - 실패")
		void createOrderFromCart_NegativePrice_Fail() {
			// given
			Member testMember = createTestMember(1L);
			OrderCreateRequest testOrderCreateRequest = createTestOrderCreateRequest();

			CartItem invalidCartItem = createTestCartItem(1L, "테스트 상품 1", -1000, 1); // 잘못된 가격
			CartResponse invalidCart = CartResponse.builder()
				.cartId("cart:1")
				.items(Arrays.asList(invalidCartItem))
				.build();

			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(cartService.getCart(1L)).willReturn(invalidCart);

			// when & then
			assertThatThrownBy(() -> orderService.createOrderFromCart(testOrderCreateRequest, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("가격이 유효하지 않습니다. 상품ID: 1");
		}
	}

	@Nested
	@DisplayName("주문 조회 테스트")
	class GetOrderTest {

		@Test
		@DisplayName("정상적인 주문 조회 - 성공")
		void getOrder_Success() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Order testOrder = createTestOrder(testMember1, testProduct1);

			given(orderRepository.findById(1L)).willReturn(Optional.of(testOrder));

			// when
			OrderResponse result = orderService.getOrder(1L, 1L);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getOrderId()).isEqualTo(1L);
			assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED.name());
			assertThat(result.getConsumerName()).isEqualTo("홍길동");
		}

		@Test
		@DisplayName("존재하지 않는 주문 조회 - 실패")
		void getOrder_OrderNotFound_Fail() {
			// given
			given(orderRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> orderService.getOrder(999L, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("주문을 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("본인이 아닌 주문 조회 - 실패")
		void getOrder_AccessDenied_Fail() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Order testOrder = createTestOrder(testMember1, testProduct1);

			given(orderRepository.findById(1L)).willReturn(Optional.of(testOrder));

			// when & then
			assertThatThrownBy(() -> orderService.getOrder(1L, 999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("본인의 주문만 조회할 수 있습니다.");
		}
	}

	@Nested
	@DisplayName("주문 목록 조회 테스트")
	class GetMyOrdersTest {

		@Test
		@DisplayName("정상적인 주문 목록 조회 - 성공")
		void getMyOrders_Success() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);

			Order testOrder1 = createTestOrder(testMember1, testProduct1);
			Order testOrder2 = createTestOrder(testMember1, testProduct1);
			testOrder2.setId(2L);

			List<Order> orders = Arrays.asList(testOrder1, testOrder2);
			Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 2);

			given(orderRepository.findByMemberId(eq(1L), any(Pageable.class))).willReturn(orderPage);

			// when
			Page<OrderResponse> result = orderService.getMyOrders(1L, PageRequest.of(0, 20));

			// then
			assertThat(result).isNotNull();
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getTotalElements()).isEqualTo(2);
			assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
			assertThat(result.getContent().get(1).getOrderId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("빈 주문 목록 조회 - 성공")
		void getMyOrders_EmptyList_Success() {
			// given
			Page<Order> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);
			given(orderRepository.findByMemberId(eq(1L), any(Pageable.class))).willReturn(emptyPage);

			// when
			Page<OrderResponse> result = orderService.getMyOrders(1L, PageRequest.of(0, 20));

			// then
			assertThat(result).isNotNull();
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("주문 취소 테스트")
	class CancelOrderTest {

		@Test
		@DisplayName("정상적인 주문 취소 - 성공")
		void cancelOrder_Success() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Order testOrder = createTestOrder(testMember1, testProduct1);

			given(orderRepository.findById(1L)).willReturn(Optional.of(testOrder));

			// when
			OrderResponse result = orderService.cancelOrder(1L, 1L);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED.name());

			// 재고 복구 확인
			assertThat(testProduct1.getStock()).isEqualTo(12); // 10 + 2
		}

		@Test
		@DisplayName("존재하지 않는 주문 취소 - 실패")
		void cancelOrder_OrderNotFound_Fail() {
			// given
			given(orderRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> orderService.cancelOrder(999L, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("주문을 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("본인이 아닌 주문 취소 - 실패")
		void cancelOrder_AccessDenied_Fail() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Order testOrder = createTestOrder(testMember1, testProduct1);

			given(orderRepository.findById(1L)).willReturn(Optional.of(testOrder));

			// when & then
			assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("본인의 주문만 취소할 수 있습니다.");
		}

		@Test
		@DisplayName("취소 불가능한 상태의 주문 취소 - 실패")
		void cancelOrder_InvalidStatus_Fail() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Order testOrder = createTestOrder(testMember1, testProduct1);
			testOrder.setStatus(OrderStatus.SHIPPED); // 취소 불가능한 상태

			given(orderRepository.findById(1L)).willReturn(Optional.of(testOrder));

			// when & then
			assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("주문 취소는 주문 생성 상태에서만 가능합니다. 현재 상태: SHIPPED");
		}
	}

	@Nested
	@DisplayName("OrderResponse 생성 테스트")
	class CreateOrderResponseTest {

		@Test
		@DisplayName("OrderResponse 정상 생성 - 성공")
		void createOrderResponse_Success() {
			// given
			Member testMember1 = createTestMember(1L);
			Product testProduct1 = createTestProduct(1L, testMember1, "테스트 상품 1", 10000, "블랙", Category.BOOKS, 10,
				true);
			Order testOrder = createTestOrder(testMember1, testProduct1);

			// when
			OrderResponse result = orderService.createOrderResponse(testOrder);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getOrderId()).isEqualTo(1L);
			assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED.name());
			assertThat(result.getConsumerName()).isEqualTo("홍길동");
			assertThat(result.getConsumerPhone()).isEqualTo("010-1234-5678");
			assertThat(result.getReceiverName()).isEqualTo("홍길동");
			assertThat(result.getReceiverPhone()).isEqualTo("010-1234-5678");
			assertThat(result.getReceiverRoadName()).isEqualTo("서울시 강남구 테헤란로 123");
			assertThat(result.getReceiverPostalCode()).isEqualTo("06292");
			assertThat(result.getItems()).hasSize(1);
			assertThat(result.getItems().get(0).getProductId()).isEqualTo(1L);
			assertThat(result.getItems().get(0).getName()).isEqualTo("테스트 상품 1");
			assertThat(result.getItems().get(0).getPriceSnapshot()).isEqualTo(10000);
			assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
			assertThat(result.getItems().get(0).getSubtotal()).isEqualTo(20000);
		}
	}
}