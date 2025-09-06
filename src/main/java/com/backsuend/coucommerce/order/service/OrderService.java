package com.backsuend.coucommerce.order.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.cart.dto.CartItem;
import com.backsuend.coucommerce.cart.dto.CartResponse;
import com.backsuend.coucommerce.cart.service.CartService;
import com.backsuend.coucommerce.catalog.entity.Product;
import com.backsuend.coucommerce.catalog.repository.ProductRepository;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.order.dto.OrderCreateRequest;
import com.backsuend.coucommerce.order.dto.OrderLineResponse;
import com.backsuend.coucommerce.order.dto.OrderResponse;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderProduct;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

/**
 * 주문 서비스
 * - Buyer(구매자)의 주문 관련 비즈니스 로직 처리
 * - 장바구니에서 주문 생성, 주문 조회, 주문 취소 기능 제공
 */
@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final CartService cartService;

	/**
	 * 장바구니에서 주문 생성
	 *
	 * @param request 주문 생성 요청 정보 (배송지 정보 포함)
	 * @param memberId 주문하는 회원 ID
	 * @return 생성된 주문 정보
	 * @throws BusinessException 회원이 없거나, 장바구니가 비어있거나, 상품 정보가 유효하지 않은 경우
	 */
	@Transactional
	public OrderResponse createOrderFromCart(OrderCreateRequest request, Long memberId) {
		// 1. Buyer 조회 및 검증
		Member buyer = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다."));

		// 2. Redis에서 장바구니 가져오기
		CartResponse cartResponse = cartService.getCart(memberId);
		if (cartResponse.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "장바구니가 비어 있습니다.");
		}

		// 2-1) 입력값 1차 검증
		for (CartItem ci : cartResponse.getItems()) {
			if (ci.getQuantity() <= 0) {
				throw new BusinessException(ErrorCode.INVALID_INPUT,
					"수량은 1 이상이어야 합니다. 상품ID: " + ci.getProductId());
			}
			if (ci.getPrice() < 0) {
				throw new BusinessException(ErrorCode.INVALID_INPUT,
					"가격이 유효하지 않습니다. 상품ID: " + ci.getProductId());
			}
		}

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

		System.out.println("	order.getId=" + order.getId());
		System.out.println("	order.getConsumerName()=" + order.getConsumerName());
		System.out.println("	order.consumerPhone()=" + order.getConsumerPhone());

		// 4. CartItem → OrderProduct 변환 + 가격/재고 검증 및 재고 차감
		for (CartItem cartItem : cartResponse.getItems()) {
			System.out.println("+++++ 가격/재고 검증 및 재고 차감");
			Product product = productRepository.findById(cartItem.getProductId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
					"상품을 찾을 수 없습니다. 상품ID: " + cartItem.getProductId()));

			// 상품 공개 상태 확인
			if (!product.isVisible()) {
				throw new BusinessException(ErrorCode.CONFLICT,
					"판매 중단된 상품입니다: " + product.getName());
			}

			System.out.println(product.getPrice() + "___" + cartItem.getPrice());
			// 가격 검증 (장바구니에 담은 시점과 주문 시점의 가격 비교)
			if (product.getPrice() != cartItem.getPrice()) {
				throw new BusinessException(ErrorCode.CONFLICT,
					"상품 가격이 변경되었습니다. 상품: " + product.getName() + ", 최신 가격: " + product.getPrice() + "원");
			}

			// 재고 검증 및 차감
			if (product.getStock() < cartItem.getQuantity()) {
				throw new BusinessException(ErrorCode.CONFLICT,
					"상품 재고가 부족합니다. 상품: " + product.getName() + ", 요청 수량: " + cartItem.getQuantity() + ", 현재 재고: "
						+ product.getStock());
			}

			// 재고 차감 (동시성 문제 고려 필요)
			product.setStock(product.getStock() - cartItem.getQuantity());

			// OrderProduct 엔티티 생성
			OrderProduct orderProduct = OrderProduct.builder()
				.product(product)
				.quantity(cartItem.getQuantity())
				.priceSnapshot(product.getPrice())
				.build();

			order.addItem(orderProduct);
		}

		// 5. 주문 저장
		Order savedOrder = orderRepository.save(order);

		// 6. 장바구니 초기화 (주문 완료 후)
		cartService.clearCart(memberId);

		// 7. 응답 반환
		return createOrderResponse(savedOrder);
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
		for (OrderProduct orderProduct : order.getItems()) {
			Product product = orderProduct.getProduct();
			product.setStock(product.getStock() + orderProduct.getQuantity());
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
}