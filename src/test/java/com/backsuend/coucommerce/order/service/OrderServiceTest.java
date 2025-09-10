package com.backsuend.coucommerce.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Map;
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

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.service.ProductSummaryService;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderDetailProduct;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderProductRepository;
import com.backsuend.coucommerce.order.repository.OrderRepository;
import com.backsuend.coucommerce.order.verification.OrderVerificationService;

@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CartService cartService;
    @Mock private OrderVerificationService orderVerificationService;
    @Mock private OrderSnapshotService orderSnapshotService;
    @Mock private OrderProductRepository orderProductRepository;
    @Mock private ProductSummaryService productSummaryService;

    @InjectMocks private OrderService orderService;

    @BeforeEach
    void setup() { MockitoAnnotations.openMocks(this); }

    private Member member(Long id) { return Member.builder().id(id).email("u"+id+"@t.com").build(); }

    private Product product(Long id, Member seller, String name, int price, int stock) {
        return Product.builder().id(id).member(seller).name(name).detail("d").price(price).stock(stock)
            .category(Category.DIGITAL).tranPrice(0).visible(true).build();
    }

    private CartItem cartItem(Long productId, String name, int price, int qty) {
        return CartItem.builder().productId(productId).productName(name).priceAtAdd(price).quantity(qty).detail("opt").build();
    }

    private OrderCreateRequest orderReq() {
        return OrderCreateRequest.builder()
            .consumerName("홍길동").consumerPhone("010-1111-2222").receiverName("홍길동")
            .receiverRoadName("서울시 어딘가 123").receiverPhone("010-1111-2222").receiverPostalCode("04538").build();
    }

    @Nested @DisplayName("주문 생성 분할")
    class CreateSplit {
        @Test @DisplayName("서로 다른 셀러 상품 2개 → 주문 2건")
        void splitBySeller() {
            Member buyer = member(1L);
            Member seller1 = member(10L);
            Member seller2 = member(20L);
            Product p1 = product(100L, seller1, "A", 1000, 10);
            Product p2 = product(200L, seller2, "B", 2000, 5);

            CartResponse cart = CartResponse.builder().items(List.of(
                cartItem(100L, "A", 1000, 2), cartItem(200L, "B", 2000, 1)
            )).build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(buyer));
            given(cartService.getCart(1L)).willReturn(cart);
            given(orderVerificationService.verify(anyList())).willReturn(Map.of(100L, p1, 200L, p2));
            // 스냅샷 서비스가 아이템을 채워주도록 스텁
            given(orderSnapshotService.toOrderProducts(any(Order.class), anyList(), anyMap()))
                .willAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    @SuppressWarnings("unchecked")
                    java.util.List<com.backsuend.coucommerce.cart.dto.CartItem> items = inv.getArgument(1);
                    @SuppressWarnings("unchecked")
                    java.util.Map<Long, com.backsuend.coucommerce.catalog.entity.Product> map = inv.getArgument(2);
                    for (CartItem ci : items) {
                        Product p = map.get(ci.getProductId());
                        OrderDetailProduct line = OrderDetailProduct.builder()
                            .order(o)
                            .product(p)
                            .quantity(ci.getQuantity())
                            .priceSnapshot(p.getPrice())
                            .build();
                        o.addItem(line);
                    }
                    return o;
                });
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
                Order o = inv.getArgument(0); o.setId(java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 10000)); return o; });

            List<OrderResponse> responses = orderService.createOrderFromCart(orderReq(), 1L);
            assertThat(responses).hasSize(2);
        }
    }

    @Nested @DisplayName("배송 완료")
    class CompleteOrder {
        @Test @DisplayName("SHIPPED → COMPLETED 및 주문수 증가 호출")
        void completeCounts() {
            Member seller = member(10L);
            Product p = product(100L, seller, "A", 1000, 10);
            Order order = Order.builder().id(1L).member(member(1L)).status(OrderStatus.SHIPPED).build();
            OrderDetailProduct line = OrderDetailProduct.builder().product(p).quantity(3).priceSnapshot(1000).build();
            order.getItems().add(line);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            OrderResponse res = orderService.completeOrder(1L, seller.getId());
            assertThat(res.getStatus()).isEqualTo(OrderStatus.COMPLETED.name());
            then(productSummaryService).should().setOrderCount(p.getId(), 3);
        }
    }

    @Nested @DisplayName("취소/배송 API")
    class CancelAndShip {
        @Test @DisplayName("PLACED 상태에서 취소 시 재고 복구")
        void cancelOrder_success() {
            Member buyer = member(1L);
            Member seller = member(10L);
            Product p = product(100L, seller, "A", 1000, 10);
            Order order = Order.builder().id(1L).member(buyer).status(OrderStatus.PLACED).build();
            order.addItem(OrderDetailProduct.builder().product(p).quantity(2).priceSnapshot(1000).build());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            OrderResponse res = orderService.cancelOrder(1L, buyer.getId());
            assertThat(res.getStatus()).isEqualTo(OrderStatus.CANCELED.name());
            assertThat(p.getStock()).isEqualTo(12);
        }

        @Test @DisplayName("PAID 상태에서 셀러 배송 처리 시 SHIPPED")
        void shipOrder_success() {
            Member buyer = member(1L);
            Member seller = member(10L);
            Product p = product(100L, seller, "A", 1000, 10);
            Order order = Order.builder().id(1L).member(buyer).status(OrderStatus.PAID).build();
            order.addItem(OrderDetailProduct.builder().product(p).quantity(1).priceSnapshot(1000).build());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            OrderResponse res = orderService.shipOrder(1L, seller.getId(), new com.backsuend.coucommerce.order.dto.ShipOrderRequest("T1","CJ"));
            assertThat(res.getStatus()).isEqualTo(OrderStatus.SHIPPED.name());
        }
    }
}
