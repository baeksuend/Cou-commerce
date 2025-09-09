package com.backsuend.coucommerce.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderDetailProduct;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderRepository;
import com.backsuend.coucommerce.payment.dto.PaymentRequest;
import com.backsuend.coucommerce.payment.dto.PaymentResponse;
import com.backsuend.coucommerce.payment.entity.CardBrand;
import com.backsuend.coucommerce.payment.entity.Payment;
import com.backsuend.coucommerce.payment.entity.PaymentStatus;
import com.backsuend.coucommerce.payment.repository.PaymentRepository;

@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("결제 성공 시 APPROVED/PAID")
    void processPayment_success() {
        Member m = Member.builder().id(1L).email("u@test.com").build();
        Order order = Order.builder().id(10L).member(m).status(OrderStatus.PLACED).build();
        OrderDetailProduct line = OrderDetailProduct.builder().quantity(2).priceSnapshot(5000).build();
        order.getItems().add(line);
        given(orderRepository.findById(10L)).willReturn(Optional.of(order));

        PaymentRequest req = PaymentRequest.builder()
            .orderId(10L).cardBrand(CardBrand.KB).amount(10000).simulate("SUCCESS").build();

        PaymentResponse res = paymentService.processPayment(1L, 10L, req);

        assertThat(res.getStatus()).isEqualTo(PaymentStatus.APPROVED.name());
        assertThat(res.getOrderStatus()).isEqualTo(OrderStatus.PAID.name());
    }

    @Test
    @DisplayName("환불 요청 시 Payment/Order 플래그 세팅")
    void requestRefund_setsFlags() {
        Member m = Member.builder().id(1L).email("u@test.com").build();
        Order order = Order.builder().id(10L).member(m).status(OrderStatus.PAID).build();
        Payment payment = Payment.builder().id(100L).order(order).status(PaymentStatus.APPROVED)
            .cardBrand(CardBrand.KB).amount(1000).build();
        given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));

        PaymentResponse res = paymentService.requestRefund(1L, 100L, "사유");
        assertThat(res).isNotNull();
        assertThat(payment.isRefundRequested()).isTrue();
        assertThat(order.isRefundRequested()).isTrue();
    }

    @Test
    @DisplayName("주문 소유자만 결제 가능")
    void processPayment_accessDenied() {
        Member owner = Member.builder().id(1L).build();
        Member attacker = Member.builder().id(9L).build();
        Order order = Order.builder().id(10L).member(owner).status(OrderStatus.PLACED).build();
        OrderDetailProduct line = OrderDetailProduct.builder().quantity(1).priceSnapshot(1000).build();
        order.getItems().add(line);
        given(orderRepository.findById(10L)).willReturn(Optional.of(order));

        PaymentRequest req = PaymentRequest.builder().orderId(10L).cardBrand(CardBrand.KB).amount(1000).simulate("SUCCESS").build();
        assertThatThrownBy(() -> paymentService.processPayment(attacker.getId(), 10L, req))
            .isInstanceOf(com.backsuend.coucommerce.common.exception.BusinessException.class)
            .hasMessageContaining("본인의 주문만 결제할 수 있습니다.");
    }

    @Test
    @DisplayName("환불 요청은 APPROVED/PAID 상태에서만 가능")
    void requestRefund_invalidState() {
        Member buyer = Member.builder().id(1L).build();
        Order order = Order.builder().id(10L).member(buyer).status(OrderStatus.PLACED).build();
        Payment payment = Payment.builder().id(100L).order(order).status(PaymentStatus.PENDING).cardBrand(CardBrand.KB).amount(1000).build();
        given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
        assertThatThrownBy(() -> paymentService.requestRefund(buyer.getId(), 100L, "사유"))
            .isInstanceOf(com.backsuend.coucommerce.common.exception.BusinessException.class)
            .hasMessageContaining("환불 요청이 불가능한 상태");
    }
}
