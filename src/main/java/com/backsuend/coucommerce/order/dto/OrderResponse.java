package com.backsuend.coucommerce.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * 주문 응답 DTO
 * - 주문 생성, 주문 조회 시 클라이언트로 반환되는 기본 응답 구조
 * - 주문의 전체 정보와 상품 목록을 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

	/**
	 * 주문 ID
	 * - 주문의 고유 식별자
	 */
	private Long orderId;

	/**
	 * 주문 상태
	 * - PLACED: 주문 생성됨
	 * - PAID: 결제 완료
	 * - SHIPPED: 배송중
	 * - COMPLETED: 배송완료
	 * - CANCELED: 취소됨
	 */
	private String status;

	/**
	 * 주문자 이름
	 * - 주문을 한 사람의 이름
	 */
	private String consumerName;

	/**
	 * 주문자 연락처
	 * - 주문자에게 연락할 수 있는 전화번호
	 */
	private String consumerPhone;

	/**
	 * 수령인 이름
	 * - 실제 배송받을 사람의 이름
	 */
	private String receiverName;

	/**
	 * 수령인 도로명 주소
	 * - 배송지 주소
	 */
	private String receiverRoadName;

	/**
	 * 수령인 연락처
	 * - 배송 시 연락할 수 있는 전화번호
	 */
	private String receiverPhone;

	/**
	 * 수령인 우편번호
	 * - 배송지 우편번호
	 */
	private String receiverPostalCode;

	/**
	 * 주문 생성 시간
	 * - 주문이 생성된 정확한 시간
	 */
	private LocalDateTime createdAt;

	/**
	 * 주문 상품 목록
	 * - 주문에 포함된 모든 상품 정보
	 */
	private List<OrderLineResponse> items;

	/**
	 * 총 주문 금액 계산
	 * @return 모든 상품의 subtotal 합계
	 */
	public int getTotalAmount() {
		return items != null ?
			items.stream()
				.mapToInt(OrderLineResponse::getSubtotal)
				.sum() : 0;
	}

	/**
	 * 주문 상품 개수 반환
	 * @return 주문 상품 개수
	 */
	public int getItemCount() {
		return items != null ? items.size() : 0;
	}

	/**
	 * 주문이 완료 상태인지 확인
	 * @return COMPLETED 상태면 true, 아니면 false
	 */
	public boolean isCompleted() {
		return "COMPLETED".equals(status);
	}

	/**
	 * 주문이 취소된 상태인지 확인
	 * @return CANCELED 상태면 true, 아니면 false
	 */
	public boolean isCanceled() {
		return "CANCELED".equals(status);
	}

	/**
	 * 주문이 배송중인지 확인
	 * @return SHIPPED 상태면 true, 아니면 false
	 */
	public boolean isShipped() {
		return "SHIPPED".equals(status);
	}

	/**
	 * 주문 정보가 유효한지 확인
	 * @return 유효하면 true, 아니면 false
	 */
	public boolean isValid() {
		return orderId != null && status != null && !status.trim().isEmpty()
			&& consumerName != null && !consumerName.trim().isEmpty()
			&& receiverName != null && !receiverName.trim().isEmpty()
			&& createdAt != null && items != null && !items.isEmpty();
	}
}