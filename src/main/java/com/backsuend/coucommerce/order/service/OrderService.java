package com.backsuend.coucommerce.order.service;

import java.util.List;
import java.util.Map;

import jakarta.persistence.OptimisticLockException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderLineResponse;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.dto.ShipOrderRequest;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderDetailProduct;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.entity.Shipment;
import com.backsuend.coucommerce.order.repository.OrderProductRepository;
import com.backsuend.coucommerce.order.repository.OrderRepository;
import com.backsuend.coucommerce.order.verification.OrderVerificationService;
import com.backsuend.coucommerce.payment.entity.Payment;
import com.backsuend.coucommerce.payment.entity.PaymentStatus;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

/**
 * 주문 서비스
 * - Buyer(구매자)의 주문 관련 비즈니스 로직 처리
 * - 장바구니에서 주문 생성, 주문 조회, 주문 취소 기능 제공
 *
 * ToDo
 * 1. Spring Retry (@Retryable) : spring-retry 라이브러리를 추가하면, 재시도를 애노테이션으로 선언할 수 있음.
 * 2. 자동 연동 모드 (추후 구현 가능) : 택배사(Open API, 예: CJ, 한진, 롯데)와 연동 배송 요청 시 API 호출 → 운송장 번호 자동 발급 시스템이 바로 Shipment 엔티티에 기록
 * 3. 상태 전이는 OrderStatus Enum으로 통제 (취소 승인 = CANCEL_REQUESTED만 허용, 환불 승인 = REFUND_REQUESTED만 허용) 추적은 OrderLog로 보강 누가 언제 "취소 요청"을 했고, "승인"을 했는지 기록 boolean 필드는 나중에 조회 최적화나 운영 편의가 필요할 때만 추가
 */

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final MemberRepository memberRepository;
	private final CartService cartService;
	private final OrderVerificationService orderVerificationService;
	private final OrderSnapshotService orderSnapshotService;
	private final OrderProductRepository orderProductRepository;
	/**
	 * 장바구니에서 주문 생성
	 * @param request 주문 생성 요청 정보 (배송지 정보 포함)
	 * @param memberId 주문하는 회원 ID
	 * @return 생성된 주문 정보
	 * @throws BusinessException 회원이 없거나, 장바구니가 비어있거나, 상품 정보가 유효하지 않은 경우
	 */

	/**
	 * TODO
	 * 1. Idempotency-Key(헤더) 지원 : 클라이언트가 주문 생성 API를 같은 요청을 여러 번 보낼 수 있음 (네트워크 타임아웃, 중복 클릭 등). 이런 경우 동일한 주문이 두 번 생성될 수 있는데, Idempotency-Key를 써서 중복 요청은 하나로만 처리하라는 뜻입니다.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public OrderResponse createOrderFromCart(OrderCreateRequest request, Long memberId) {
		int maxRetries = 3;
		int attempt = 0;

		while (true) {
			try {
				attempt++;
				// 1. Buyer 조회 및 검증
				Member buyer = memberRepository.findById(memberId)
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다."));

				// 2. Redis에서 장바구니 가져오기
				CartResponse cartResponse = cartService.getCart(memberId);
				if (cartResponse.isEmpty()) {
					throw new BusinessException(ErrorCode.NOT_FOUND, "장바구니가 비어 있습니다.");
				}
				// 3. 최신 가격/재고 재검증
				Map<Long, Product> productMap = orderVerificationService.verify(cartResponse.getItems());

				// 3. Order 엔티티 생성
				Order order = Order.builder()
					.member(buyer)
					.consumerName(request.getConsumerName())
					.consumerPhone(request.getConsumerPhone())
					.receiverName(request.getReceiverName())
					.receiverRoadName(request.getReceiverRoadName())
					.receiverPhone(request.getReceiverPhone())
					.receiverPostalCode(request.getReceiverPostalCode())
					.build();

				// 스냅샷 저장: Product.price -> OrderProduct.priceSnapshot, Product 참조 유지
				order = orderSnapshotService.toOrderProducts(order, cartResponse.getItems(), productMap);

				// 주문 저장
				Order savedOrder = orderRepository.save(order);

				// 상세 주문 스냅샷 저장
				orderProductRepository.saveAll(order.getItems());

				// 장바구니 초기화 (주문 완료 후)
				cartService.clearCart(memberId);

				// 응답 반환
				return createOrderResponse(savedOrder);
			} catch (OptimisticLockException e) {
				if (attempt >= maxRetries) {
					throw new BusinessException(ErrorCode.CONFLICT, "동시 주문 충돌. 다시 시도해주세요.");
				}
				try {
					Thread.sleep(50L * attempt); // backoff
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * 주문 상세 조회 (본인 주문만 조회 가능)
	 *
	 * @param orderId 조회할 주문 ID
	 * @param memberId 요청한 회원 ID
	 * @return 주문 상세 정보
	 * @throws BusinessException 주문이 없거나, 본인의 주문이 아닌 경우
	 */
	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long orderId, Long memberId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

		// 본인 주문인지 확인 (보안 검증)
		if (!order.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 주문만 조회할 수 있습니다.");
		}

		return createOrderResponse(order);
	}

	/**
	 * Buyer의 주문 목록 조회 (페이징)
	 *
	 * @param memberId 조회할 회원 ID
	 * @param pageable 페이징 정보
	 * @return 주문 목록 (페이징 적용)
	 */
	@Transactional(readOnly = true)
	public Page<OrderResponse> getMyOrders(Long memberId, Pageable pageable) {
		Page<Order> orders = orderRepository.findByMemberId(memberId, pageable);
		return orders.map(this::createOrderResponse);
	}

	/**
	 * 주문 취소
	 *
	 * @param orderId 취소할 주문 ID
	 * @param memberId 요청한 회원 ID
	 * @return 취소된 주문 정보
	 * @throws BusinessException 주문이 없거나, 본인의 주문이 아니거나, 취소 불가능한 상태인 경우
	 */
	@Transactional
	public OrderResponse cancelOrder(Long orderId, Long memberId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

		// 본인 주문인지 확인 (보안 검증)
		if (!order.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 주문만 취소할 수 있습니다.");
		}

		// 주문 상태 확인 (PLACED 상태에서만 취소 가능)
		if (order.getStatus() != OrderStatus.PLACED) {
			throw new BusinessException(ErrorCode.CONFLICT,
				"주문 취소는 주문 생성 상태에서만 가능합니다. 현재 상태: " + order.getStatus());
		}

		// 재고 복구 (취소 시 재고 반환)
		for (OrderDetailProduct orderDetailProduct : order.getItems()) {
			Product product = orderDetailProduct.getProduct();
			product.setStock(product.getStock() + orderDetailProduct.getQuantity());
		}

		// 주문 상태 변경
		order.setStatus(OrderStatus.CANCELED);

		return createOrderResponse(order);
	}

	/**
	 * OrderResponse 생성 헬퍼 메서드
	 *
	 * @param order Order 엔티티
	 * @return OrderResponse DTO
	 */
	OrderResponse createOrderResponse(Order order) {

		return OrderResponse.builder()
			.orderId(order.getId())
			.status(order.getStatus().name())
			.consumerName(order.getConsumerName())
			.consumerPhone(order.getConsumerPhone())
			.receiverName(order.getReceiverName())
			.receiverRoadName(order.getReceiverRoadName())
			.receiverPhone(order.getReceiverPhone())
			.receiverPostalCode(order.getReceiverPostalCode())
			.createdAt(order.getCreatedAt())
			.items(order.getItems().stream()
				.map(op -> OrderLineResponse.builder()
					.productId(op.getProduct().getId())
					.name(op.getProduct().getName())
					.priceSnapshot(op.getPriceSnapshot())
					.quantity(op.getQuantity())
					.subtotal(op.getPriceSnapshot() * op.getQuantity())
					.build())
				.toList())
			.build();
	}

	@Transactional
	public OrderResponse shipOrder(Long orderId, Long sellerId, ShipOrderRequest request) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문 없음"));

		// Seller 검증: 본인 상품 포함 여부 확인
		boolean ownsProduct = order.getItems().stream()
			.anyMatch(item -> item.getProduct().getMember().getId().equals(sellerId));
		if (!ownsProduct) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인 상품 주문만 배송 처리 가능");
		}

		// 상태 검증
		if (order.getStatus() != OrderStatus.PAID) {
			throw new BusinessException(ErrorCode.CONFLICT, "배송은 PAID 상태에서만 가능");
		}

		// Shipment 생성
		Shipment shipment = Shipment.builder()
			.order(order)
			.trackingNo(request.getTrackingNo())
			.carrier(request.getCarrier())
			.build();

		// 상태 변경
		order.setStatus(OrderStatus.SHIPPED);
		order.setShipment(shipment); // 편의 메서드 추가 필요

		return createOrderResponse(order);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getSellerOrders(Long sellerId) {
		List<Order> orders = orderRepository.findBySellerId(sellerId);
		// 👉 커스텀 쿼리 필요: OrderDetailProduct.product.member.id = :sellerId

		return orders.stream()
			.map(this::createOrderResponse)
			.toList();
	}

	@Transactional
	public OrderResponse approveCancel(Long orderId, Long sellerId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문 없음"));

		boolean ownsProduct = order.getItems().stream()
			.anyMatch(item -> item.getProduct().getMember().getId().equals(sellerId));
		if (!ownsProduct) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인 상품 주문만 처리 가능");
		}

		if (!order.isCancelRequested()) {
			throw new BusinessException(ErrorCode.CONFLICT, "현재 상태에서 취소 승인 불가");
		}

		order.setStatus(OrderStatus.CANCELED);
		return createOrderResponse(order);
	}

	@Transactional
	public OrderResponse approveRefund(Long orderId, Long sellerId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문 없음"));

		boolean ownsProduct = order.getItems().stream()
			.anyMatch(item -> item.getProduct().getMember().getId().equals(sellerId));
		if (!ownsProduct) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인 상품 주문만 처리 가능");
		}

		if (!order.isRefundRequested()) {
			throw new BusinessException(ErrorCode.CONFLICT, "현재 상태에서 환불 승인 불가");
		}

		// 상태 전이
		order.setStatus(OrderStatus.REFUNDED);
		order.setRefundRequested(false);

		Payment payment = order.getPayment();
		if (payment != null) {
			payment.setStatus(PaymentStatus.REFUNDED);
			payment.setRefundRequested(false);
		}

		return createOrderResponse(order);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getAllOrders(Long buyerId, Long sellerId, OrderStatus status) {
		return orderRepository.findAllByFilters(buyerId, sellerId, status).stream()
			.map(this::createOrderResponse)
			.toList();
	}

	@Transactional
	public OrderResponse forceCancel(Long orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문 없음"));
		order.setStatus(OrderStatus.CANCELED);
		order.setRefundRequested(false);
		Payment payment = order.getPayment();
		payment.setStatus(PaymentStatus.REFUNDED);
		payment.setRefundRequested(false);
		return createOrderResponse(order);
	}

	@Transactional
	public OrderResponse forceRefund(Long orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문 없음"));
		order.setStatus(OrderStatus.REFUNDED);
		order.setRefundRequested(false);
		Payment payment = order.getPayment();
		payment.setStatus(PaymentStatus.REFUNDED);
		payment.setRefundRequested(false);
		return createOrderResponse(order);
	}
}