package com.backsuend.coucommerce.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.common.exception.BusinessException;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private PaymentService paymentService;

	private Member testMember;
	private Product testProduct;
	private Order testOrder;
	private OrderDetailProduct testOrderDetailProduct;
	private PaymentRequest paymentRequest;

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

		testProduct = new Product();
		testProduct.setId(1L);
		testProduct.setName("테스트 상품");
		testProduct.setPrice(10000);
		testProduct.setStock(10);
		testProduct.setVisible(true);
		testProduct.setCategory(Category.DIGITAL);

		testOrderDetailProduct = OrderDetailProduct.builder()
			.id(1L)
			.product(testProduct)
			.quantity(2)
			.priceSnapshot(10000)
			.build();

		testOrder = Order.builder()
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

		testOrder.getItems().add(testOrderDetailProduct);
		testOrderDetailProduct.setOrder(testOrder);

		paymentRequest = new PaymentRequest();
		paymentRequest.setCardBrand(CardBrand.VISA);
		paymentRequest.setAmount(20000); // 10000 * 2
		paymentRequest.setSimulate("SUCCESS");
	}

	@Test
	@DisplayName("결제 처리 성공")
	void processPayment_Success() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(null);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(1L);
			return payment;
		});

		// When
		PaymentResponse result = paymentService.processPayment(memberId, orderId, paymentRequest);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getPaymentId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED.name());
		assertThat(result.getAmount()).isEqualTo(20000);
		assertThat(result.getCardBrand()).isEqualTo(CardBrand.VISA.name());
		assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	@DisplayName("결제 처리 실패")
	void processPayment_Failed() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		paymentRequest.setSimulate("FAIL");

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(null);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(1L);
			return payment;
		});

		// When
		PaymentResponse result = paymentService.processPayment(memberId, orderId, paymentRequest);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED.name());
		assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PLACED); // 상태 변경 없음
	}

	@Test
	@DisplayName("존재하지 않는 주문으로 결제 처리 시 예외 발생")
	void processPayment_OrderNotFound() {
		// Given
		Long memberId = 1L;
		Long orderId = 999L;
		when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> paymentService.processPayment(memberId, orderId, paymentRequest))
			.isInstanceOf(BusinessException.class)
			.hasMessage("주문을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("다른 사용자의 주문으로 결제 처리 시 예외 발생")
	void processPayment_AccessDenied() {
		// Given
		Long memberId = 2L; // 다른 사용자
		Long orderId = 1L;
		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

		// When & Then
		assertThatThrownBy(() -> paymentService.processPayment(memberId, orderId, paymentRequest))
			.isInstanceOf(BusinessException.class)
			.hasMessage("본인의 주문만 결제할 수 있습니다.");
	}

	@Test
	@DisplayName("이미 결제된 주문으로 결제 처리 시 예외 발생")
	void processPayment_OrderAlreadyPaid() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		testOrder.setStatus(OrderStatus.PAID);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

		// When & Then
		assertThatThrownBy(() -> paymentService.processPayment(memberId, orderId, paymentRequest))
			.isInstanceOf(BusinessException.class)
			.hasMessage("해당 주문은 결제할 수 없는 상태입니다. 현재 상태: PAID");
	}

	@Test
	@DisplayName("이미 결제가 진행된 주문으로 결제 처리 시 예외 발생")
	void processPayment_AlreadyProcessed() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		Payment existingPayment = Payment.builder()
			.id(1L)
			.order(testOrder)
			.status(PaymentStatus.APPROVED)
			.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(existingPayment);

		// When & Then
		assertThatThrownBy(() -> paymentService.processPayment(memberId, orderId, paymentRequest))
			.isInstanceOf(BusinessException.class)
			.hasMessage("이미 결제가 진행된 주문입니다.");
	}

	@Test
	@DisplayName("결제 금액이 주문 총액과 일치하지 않을 때 예외 발생")
	void processPayment_AmountMismatch() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		paymentRequest.setAmount(15000); // 잘못된 금액

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> paymentService.processPayment(memberId, orderId, paymentRequest))
			.isInstanceOf(BusinessException.class)
			.hasMessage("결제 금액이 주문 총액과 일치하지 않습니다. 요청 금액: 15000, 주문 총액: 20000");
	}

	@Test
	@DisplayName("결제 정보 조회 성공")
	void getPayment_Success() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		Payment payment = Payment.builder()
			.id(1L)
			.order(testOrder)
			.cardBrand(CardBrand.VISA)
			.amount(20000)
			.status(PaymentStatus.APPROVED)
			.transactionId("MOCK-123456789")
			.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);

		// When
		PaymentResponse result = paymentService.getPayment(memberId, orderId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getPaymentId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED.name());
		assertThat(result.getAmount()).isEqualTo(20000);
		assertThat(result.getCardBrand()).isEqualTo(CardBrand.VISA.name());
	}

	@Test
	@DisplayName("존재하지 않는 주문의 결제 정보 조회 시 예외 발생")
	void getPayment_OrderNotFound() {
		// Given
		Long memberId = 1L;
		Long orderId = 999L;
		when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> paymentService.getPayment(memberId, orderId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("주문을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("다른 사용자의 주문 결제 정보 조회 시 예외 발생")
	void getPayment_AccessDenied() {
		// Given
		Long memberId = 2L; // 다른 사용자
		Long orderId = 1L;
		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

		// When & Then
		assertThatThrownBy(() -> paymentService.getPayment(memberId, orderId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("본인의 주문만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("결제 정보가 없는 주문 조회 시 예외 발생")
	void getPayment_PaymentNotFound() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> paymentService.getPayment(memberId, orderId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("해당 주문의 결제 정보를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("내 결제 내역 조회 성공")
	void getMyPayments_Success() {
		// Given
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		Payment payment = Payment.builder()
			.id(1L)
			.order(testOrder)
			.cardBrand(CardBrand.VISA)
			.amount(20000)
			.status(PaymentStatus.APPROVED)
			.transactionId("MOCK-123456789")
			.build();

		Page<Payment> paymentPage = new PageImpl<>(Arrays.asList(payment));
		when(paymentRepository.findByOrderMemberId(memberId, pageable)).thenReturn(paymentPage);

		// When
		Page<PaymentResponse> result = paymentService.getMyPayments(memberId, pageable);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getPaymentId()).isEqualTo(1L);
		assertThat(result.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.APPROVED.name());
	}

	@Test
	@DisplayName("다양한 카드 브랜드로 결제 처리")
	void processPayment_DifferentCardBrands() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		CardBrand[] cardBrands = {CardBrand.VISA, CardBrand.KB, CardBrand.SH, CardBrand.KAKAO};

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(null);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(1L);
			return payment;
		});

		// When & Then
		for (CardBrand cardBrand : cardBrands) {
			// 각 테스트마다 새로운 주문 상태로 리셋
			testOrder.setStatus(OrderStatus.PLACED);
			paymentRequest.setCardBrand(cardBrand);
			PaymentResponse result = paymentService.processPayment(memberId, orderId, paymentRequest);

			assertThat(result).isNotNull();
			assertThat(result.getCardBrand()).isEqualTo(cardBrand.name());
		}
	}

	@Test
	@DisplayName("결제 금액이 0원일 때 예외 발생")
	void processPayment_ZeroAmount() {
		// Given
		Long memberId = 1L;
		Long orderId = 1L;
		paymentRequest.setAmount(0);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> paymentService.processPayment(memberId, orderId, paymentRequest))
			.isInstanceOf(BusinessException.class)
			.hasMessage("결제 금액이 주문 총액과 일치하지 않습니다. 요청 금액: 0, 주문 총액: 20000");
	}
}
