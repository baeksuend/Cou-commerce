package com.backsuend.coucommerce.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.backsuend.coucommerce.BaseIntegrationTest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.catalog.entity.Category;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.member.repository.AddressRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderRepository;
import com.backsuend.coucommerce.payment.dto.PaymentRequest;
import com.backsuend.coucommerce.payment.entity.CardBrand;
import com.backsuend.coucommerce.payment.entity.Payment;
import com.backsuend.coucommerce.payment.entity.PaymentStatus;
import com.backsuend.coucommerce.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("Order, Payment, Cart 통합 테스트")
class OrderPaymentCartIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private AddressRepository addressRepository;

	private String accessToken;
	private Member testMember;
	private Product testProduct1;
	private Product testProduct2;

	@BeforeEach
	void setUp() throws Exception {
		// 테스트 데이터 초기화
		orderRepository.deleteAll();
		paymentRepository.deleteAll();
		productRepository.deleteAll();
		addressRepository.deleteAll();
		memberRepository.deleteAll();

		// 회원가입 및 로그인
		accessToken = registerAndLogin("test@example.com", "password123", "테스트 사용자", "010-1234-5678");
		testMember = memberRepository.findByEmail("test@example.com").orElseThrow();

		// 테스트 상품 생성
		testProduct1 = createTestProduct("테스트 상품 1", 10000, 10, Category.DIGITAL);
		testProduct2 = createTestProduct("테스트 상품 2", 20000, 5, Category.FASHION);
	}

	@Test
	@DisplayName("장바구니 → 주문 → 결제 전체 플로우 테스트")
	void completeOrderFlow_Success() throws Exception {
		// 1. 장바구니에 상품 추가
		addToCart(testProduct1.getId(), 2);
		addToCart(testProduct2.getId(), 1);

		// 2. 장바구니 조회 확인
		MvcResult cartResult = mockMvc.perform(get("/api/v1/cart/")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode cartResponse = objectMapper.readTree(cartResult.getResponse().getContentAsString());
		assertThat(cartResponse.get("data").get("items")).hasSize(2);

		// 3. 주문 생성
		OrderCreateRequest orderRequest = createOrderRequest();
		MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(orderRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode orderResponse = objectMapper.readTree(orderResult.getResponse().getContentAsString());
		Long orderId = orderResponse.get("data").get("orderId").asLong();

		// 4. 주문 생성 후 장바구니가 비어있는지 확인
		mockMvc.perform(get("/api/v1/cart/")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isEmpty());

		// 5. 주문 상세 조회
		mockMvc.perform(get("/api/v1/orders/" + orderId)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderId").value(orderId))
			.andExpect(jsonPath("$.data.status").value(OrderStatus.PLACED.name()))
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.items.length()").value(2));

		// 6. 결제 처리
		PaymentRequest paymentRequest = createPaymentRequest(40000); // 10000*2 + 20000*1
		MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments/" + orderId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(paymentRequest)))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode paymentResponse = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
		assertThat(paymentResponse.get("data").get("status").asText()).isEqualTo(PaymentStatus.APPROVED.name());

		// 7. 결제 후 주문 상태 확인
		mockMvc.perform(get("/api/v1/orders/" + orderId)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value(OrderStatus.PAID.name()));

		// 8. 데이터베이스 상태 확인
		Order savedOrder = orderRepository.findById(orderId).orElseThrow();
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(savedOrder.getItems()).hasSize(2);

		Payment savedPayment = paymentRepository.findByOrderId(orderId);
		assertThat(savedPayment).isNotNull();
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(savedPayment.getAmount()).isEqualTo(40000);

		// 9. 재고 차감 확인
		Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElseThrow();
		Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElseThrow();
		assertThat(updatedProduct1.getStock()).isEqualTo(8); // 10 - 2
		assertThat(updatedProduct2.getStock()).isEqualTo(4); // 5 - 1
	}

	@Test
	@DisplayName("결제 실패 시나리오 테스트")
	void paymentFailure_Scenario() throws Exception {
		// 테스트용 상품 새로 생성 (독립적인 데이터)
		Product paymentTestProduct = createTestProduct("결제 테스트 상품", 10000, 10, Category.DIGITAL);
		
		// 1. 장바구니에 상품 추가
		addToCart(paymentTestProduct.getId(), 1);

		// 2. 주문 생성
		OrderCreateRequest orderRequest = createOrderRequest();
		MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(orderRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode orderResponse = objectMapper.readTree(orderResult.getResponse().getContentAsString());
		Long orderId = orderResponse.get("data").get("orderId").asLong();

		// 3. 결제 실패 처리
		PaymentRequest paymentRequest = createPaymentRequest(10000);
		paymentRequest.setSimulate("FAIL");

		MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments/" + orderId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(paymentRequest)))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode paymentResponse = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
		assertThat(paymentResponse.get("data").get("status").asText()).isEqualTo(PaymentStatus.FAILED.name());

		// 4. 결제 실패 후 주문 상태는 PLACED로 유지
		Order savedOrder = orderRepository.findById(orderId).orElseThrow();
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);

		Payment savedPayment = paymentRepository.findByOrderId(orderId);
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
	}

	@Test
	@DisplayName("주문 취소 시나리오 테스트")
	void orderCancellation_Scenario() throws Exception {
		// 1. 장바구니에 상품 추가
		addToCart(testProduct1.getId(), 3);

		// 2. 주문 생성
		OrderCreateRequest orderRequest = createOrderRequest();
		MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(orderRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode orderResponse = objectMapper.readTree(orderResult.getResponse().getContentAsString());
		Long orderId = orderResponse.get("data").get("orderId").asLong();

		// 3. 주문 취소
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value(OrderStatus.CANCELED.name()));

		// 4. 취소 후 재고 복구 확인
		Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
		assertThat(updatedProduct.getStock()).isEqualTo(10); // 원래 재고로 복구

		// 5. 취소된 주문은 결제할 수 없음
		PaymentRequest paymentRequest = createPaymentRequest(30000);
		mockMvc.perform(post("/api/v1/payments/" + orderId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(paymentRequest)))
			.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("재고 부족 시나리오 테스트")
	void insufficientStock_Scenario() throws Exception {
		// 테스트용 상품 새로 생성 (독립적인 데이터)
		Product stockTestProduct = createTestProduct("재고 테스트 상품", 10000, 10, Category.DIGITAL);
		
		// 1. 재고보다 많은 수량으로 장바구니에 추가
		addToCart(stockTestProduct.getId(), 15); // 재고는 10개

		// 2. 주문 생성 시 재고 부족으로 실패
		OrderCreateRequest orderRequest = createOrderRequest();
		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(orderRequest)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.data.message").value("상품 재고가 부족합니다. 상품: 재고 테스트 상품, 요청 수량: 15, 현재 재고: 10"));
	}

	@Test
	@DisplayName("가격 변경 시나리오 테스트")
	void priceChange_Scenario() throws Exception {
		// 테스트용 상품 새로 생성 (독립적인 데이터)
		Product priceTestProduct = createTestProduct("가격 테스트 상품", 10000, 10, Category.DIGITAL);
		
		// 1. 장바구니에 상품 추가
		addToCart(priceTestProduct.getId(), 2);

		// 2. 상품 가격 변경
		priceTestProduct.setPrice(15000);
		productRepository.save(priceTestProduct);

		// 3. 주문 생성 시 가격 불일치로 실패
		OrderCreateRequest orderRequest = createOrderRequest();
		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(orderRequest)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.data.message").value("상품 가격이 변경되었습니다. 상품: 가격 테스트 상품, 최신 가격: 15000원"));
	}

	@Test
	@DisplayName("다른 사용자의 주문 접근 시나리오 테스트")
	void unauthorizedAccess_Scenario() throws Exception {
		// 1. 첫 번째 사용자로 주문 생성
		addToCart(testProduct1.getId(), 1);
		OrderCreateRequest orderRequest = createOrderRequest();
		MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(orderRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode orderResponse = objectMapper.readTree(orderResult.getResponse().getContentAsString());
		Long orderId = orderResponse.get("data").get("orderId").asLong();

		// 2. 두 번째 사용자 생성 및 로그인
		String secondUserToken = registerAndLogin("test2@example.com", "password123", "테스트 사용자2", "010-9876-5432");

		// 3. 두 번째 사용자가 첫 번째 사용자의 주문에 접근 시도
		mockMvc.perform(get("/api/v1/orders/" + orderId)
				.header("Authorization", "Bearer " + secondUserToken))
			.andExpect(status().isForbidden());

		// 4. 두 번째 사용자가 첫 번째 사용자의 주문에 결제 시도
		PaymentRequest paymentRequest = createPaymentRequest(10000);
		mockMvc.perform(post("/api/v1/payments/" + orderId)
				.header("Authorization", "Bearer " + secondUserToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(paymentRequest)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("내 주문 목록 조회 테스트")
	void getMyOrders_Success() throws Exception {
		// 1. 여러 주문 생성
		for (int i = 0; i < 3; i++) {
			addToCart(testProduct1.getId(), 1);
			OrderCreateRequest orderRequest = createOrderRequest();
			mockMvc.perform(post("/api/v1/orders")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(orderRequest)))
				.andExpect(status().isCreated());
		}

		// 2. 내 주문 목록 조회
		mockMvc.perform(get("/api/v1/orders/my")
				.header("Authorization", "Bearer " + accessToken)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.totalElements").value(3));
	}

	private void addToCart(Long productId, int quantity) throws Exception {
		CartItem cartItem = new CartItem();
		cartItem.setProductId(productId);
		cartItem.setQuantity(quantity);
		cartItem.setPrice(productRepository.findById(productId).orElseThrow().getPrice());

		mockMvc.perform(post("/api/v1/cart/items")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cartItem)))
			.andExpect(status().isCreated());
	}

	private Product createTestProduct(String name, int price, int stock, Category category) {
		Product product = new Product();
		product.setName(name);
		product.setPrice(price);
		product.setStock(stock);
		product.setCategory(category);
		product.setVisible(true);
		product.setDetail("테스트 상품 설명");
		product.setMember(testMember);
		return productRepository.save(product);
	}

	private OrderCreateRequest createOrderRequest() {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setConsumerName("구매자");
		request.setConsumerPhone("010-1234-5678");
		request.setReceiverName("수령자");
		request.setReceiverRoadName("서울시 강남구 테헤란로 123");
		request.setReceiverPhone("010-9876-5432");
		request.setReceiverPostalCode("12345");
		return request;
	}

	private PaymentRequest createPaymentRequest(int amount) {
		PaymentRequest request = new PaymentRequest();
		request.setCardBrand(CardBrand.VISA);
		request.setAmount(amount);
		request.setSimulate("SUCCESS");
		return request;
	}
}
