package com.backsuend.coucommerce.payment.service;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.order.entity.Order;
import com.backsuend.coucommerce.order.entity.OrderStatus;
import com.backsuend.coucommerce.order.repository.OrderRepository;
import com.backsuend.coucommerce.payment.dto.PaymentRequest;
import com.backsuend.coucommerce.payment.dto.PaymentResponse;
import com.backsuend.coucommerce.payment.entity.Payment;
import com.backsuend.coucommerce.payment.entity.PaymentStatus;
import com.backsuend.coucommerce.payment.logging.PaymentLogContext;
import com.backsuend.coucommerce.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * @author rua
 */

/**
 * Payment Service
 * - 주문에 대한 결제 처리 담당
 * - Mock API 기반 승인/실패 시뮬레이션
 *
 * ToDo
 * 1. PaymentLog : 트랜잭션 추적성 확보 (누가 언제 어떤 결제 이벤트 발생시켰는지 기록), 추후 Admin 모니터링/디버깅 용도
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final OrderRepository orderRepository;

	/**
	 * 결제 진행
	 * @param orderId 주문 ID
	 * @param request 결제 요청 DTO
	 * @return PaymentResponse
	 */
	@Transactional
	public PaymentResponse processPayment(Long memberId, Long orderId, PaymentRequest request) {

		// 1. 주문 조회 및 권한 검증
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

		// 본인 주문인지 확인 (권한 검증)
		if (!order.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 주문만 결제할 수 있습니다.");
		}

		// 2. 주문 상태 검증
		if (order.getStatus() != OrderStatus.PLACED) {
			throw new BusinessException(ErrorCode.CONFLICT, "해당 주문은 결제할 수 없는 상태입니다. 현재 상태: " + order.getStatus());
		}

		// 3. 이미 결제가 진행된 주문인지 확인
		Payment existingPayment = paymentRepository.findByOrderId(orderId);
		if (existingPayment != null) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 결제가 진행된 주문입니다.");
		}

		// 4. 결제 금액 검증
		int totalAmount = order.getItems().stream()
			.mapToInt(op -> op.getPriceSnapshot() * op.getQuantity())
			.sum();

		if (request.getAmount() != totalAmount) {
			throw new BusinessException(ErrorCode.CONFLICT,
				"결제 금액이 주문 총액과 일치하지 않습니다. 요청 금액: " + request.getAmount() +
					", 주문 총액: " + totalAmount);
		}

		// 5. Payment 엔티티 생성
		Payment payment = Payment.builder()
			.order(order)
			.cardBrand(request.getCardBrand())
			.amount(request.getAmount())
			.transactionId("MOCK-" + System.currentTimeMillis())
			.status(PaymentStatus.PENDING)
			.build();

		// 6. Mock 결제 승인/실패 처리
		if ("SUCCESS".equalsIgnoreCase(request.getSimulate())) {
			payment.setStatus(PaymentStatus.APPROVED);
			order.setStatus(OrderStatus.PAID);
		} else {
			payment.setStatus(PaymentStatus.FAILED);
		}

		Payment saved = paymentRepository.save(payment);

		try {
			PaymentLogContext.setPaymentContext(saved.getId(), request.getCardBrand().toString(),
				saved.getStatus().name());
			MDC.put("orderId", String.valueOf(orderId));
			MDC.put("amount", String.valueOf(saved.getAmount()));
			log.info("payment.process: result={}", saved.getStatus());
		} finally {
			MDC.remove("orderId");
			MDC.remove("amount");
			PaymentLogContext.clear();
		}

		return PaymentResponse.from(saved);
	}

	/**
	 * 특정 주문의 결제 정보 조회
	 *
	 * @param memberId 조회 요청한 회원 ID
	 * @param orderId 주문 ID
	 * @return PaymentResponse
	 * @throws BusinessException 결제 정보가 없거나, 본인의 주문이 아닌 경우
	 */
	@Transactional(readOnly = true)
	public PaymentResponse getPayment(Long memberId, Long orderId) {
		// 1. 주문 조회 및 권한 검증
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

		// 본인 주문인지 확인 (권한 검증)
		if (!order.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 주문만 조회할 수 있습니다.");
		}

		// 2. 결제 정보 조회
		Payment payment = paymentRepository.findByOrderId(orderId);
		if (payment == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "해당 주문의 결제 정보를 찾을 수 없습니다.");
		}

		try {
			PaymentLogContext.setPaymentContext(payment.getId(), payment.getCardBrand().toString(),
				payment.getStatus().name());
			MDC.put("orderId", String.valueOf(orderId));
			log.info("payment.get: result=found");
		} finally {
			MDC.remove("orderId");
			PaymentLogContext.clear();
		}

		return PaymentResponse.from(payment);
	}

	/**
	 * Buyer의 모든 결제 내역 조회 (페이징)
	 *
	 * @param memberId 조회할 회원 ID
	 * @param pageable 페이징 정보
	 * @return Page<PaymentResponse>
	 */
	@Transactional(readOnly = true)
	public Page<PaymentResponse> getMyPayments(Long memberId, Pageable pageable) {
		Page<Payment> payments = paymentRepository.findByOrderMemberId(memberId, pageable);
		Page<PaymentResponse> page = payments.map(PaymentResponse::from);
		log.info("payment.list: result_count={}", page.getNumberOfElements());
		return page;
	}

	@Transactional
	public PaymentResponse requestRefund(Long buyerId, Long paymentId, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "결제를 찾을 수 없습니다."));

		// Buyer 본인 검증
		if (!payment.getOrder().getMember().getId().equals(buyerId)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인 결제만 환불 요청할 수 있습니다.");
		}

		// 상태 검증
		if (payment.getStatus() != PaymentStatus.APPROVED ||
			payment.getOrder().getStatus() != OrderStatus.PAID) {
			throw new BusinessException(ErrorCode.CONFLICT, "환불 요청이 불가능한 상태입니다.");
		}

		// 상태 전이
		payment.getOrder().setRefundRequested(true);
		payment.setRefundRequested(true);
		payment.setRefundReason(reason);

		try {
			PaymentLogContext.setPaymentContext(payment.getId(), payment.getCardBrand().toString(),
				payment.getStatus().name());
			MDC.put("orderId", String.valueOf(payment.getOrder().getId()));
			log.info("payment.refund.request: result=requested");
		} finally {
			MDC.remove("orderId");
			PaymentLogContext.clear();
		}

		return PaymentResponse.from(payment);
	}
}
