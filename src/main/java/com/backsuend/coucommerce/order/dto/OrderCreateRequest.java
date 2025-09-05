package com.backsuend.coucommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */

/**
 * 주문 생성 요청 DTO
 * - Buyer(구매자)가 장바구니에서 "주문하기" 버튼 클릭 시 사용
 * - OrderService.createOrderFromCart(...) 의 입력 값으로 활용
 * - 주문 상품은 장바구니에서 자동으로 가져옴
 * - 배송 정보만 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

	/**
	 * 주문자 이름 (보내는 사람)
	 * - 주문서에 표시될 주문자 정보
	 * - 최대 20자까지 입력 가능
	 */
	@NotBlank(message = "주문자 이름은 필수입니다.")
	@Size(max = 20, message = "주문자 이름은 최대 20자까지 가능합니다.")
	@Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "주문자 이름은 한글, 영문, 공백만 입력 가능합니다.")
	private String consumerName;

	/**
	 * 주문자 연락처
	 * - 주문 관련 문의 시 사용될 연락처
	 * - 하이픈(-) 포함 형식 지원
	 */
	@NotBlank(message = "주문자 연락처는 필수입니다.")
	@Size(max = 20, message = "연락처는 최대 20자까지 가능합니다.")
	@Pattern(regexp = "^[0-9-]+$", message = "연락처는 숫자와 하이픈(-)만 입력 가능합니다.")
	private String consumerPhone;

	/**
	 * 수령인 이름
	 * - 실제 배송받을 사람의 이름
	 * - 최대 20자까지 입력 가능
	 */
	@NotBlank(message = "수령인 이름은 필수입니다.")
	@Size(max = 20, message = "수령인 이름은 최대 20자까지 가능합니다.")
	@Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "수령인 이름은 한글, 영문, 공백만 입력 가능합니다.")
	private String receiverName;

	/**
	 * 수령인 도로명 주소
	 * - 실제 배송지 주소 (도로명 주소)
	 * - 최대 100자까지 입력 가능
	 */
	@NotBlank(message = "도로명 주소는 필수입니다.")
	@Size(max = 100, message = "도로명 주소는 최대 100자까지 가능합니다.")
	private String receiverRoadName;

	/**
	 * 수령인 연락처
	 * - 배송 시 연락할 수 있는 수령인 연락처
	 * - 하이픈(-) 포함 형식 지원
	 */
	@NotBlank(message = "수령인 연락처는 필수입니다.")
	@Size(max = 20, message = "연락처는 최대 20자까지 가능합니다.")
	@Pattern(regexp = "^[0-9-]+$", message = "연락처는 숫자와 하이픈(-)만 입력 가능합니다.")
	private String receiverPhone;

	/**
	 * 수령인 우편번호
	 * - 배송지 우편번호 (5자리 또는 6자리)
	 * - 최대 10자까지 입력 가능
	 */
	@NotBlank(message = "우편번호는 필수입니다.")
	@Size(max = 10, message = "우편번호는 최대 10자까지 가능합니다.")
	@Pattern(regexp = "^[0-9-]+$", message = "우편번호는 숫자와 하이픈(-)만 입력 가능합니다.")
	private String receiverPostalCode;
}