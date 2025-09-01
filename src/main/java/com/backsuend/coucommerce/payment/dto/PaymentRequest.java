package com.backsuend.coucommerce.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.backsuend.coucommerce.payment.entity.CardBrand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author rua
 */

/**
 * 결제 요청 DTO
 * - Buyer가 결제를 진행할 때 전달하는 데이터
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

	/**
	 * 카드 브랜드 (KB, SH, KAKAO)
	 * - enum CardBrand 값 중 하나
	 * - 반드시 선택되어야 함
	 */
	@NotNull(message = "카드 브랜드는 필수 입력입니다.")
	private CardBrand cardBrand;

	/**
	 * 결제 금액
	 * - 0 이상이어야 하며
	 * - 반드시 주문 총액과 일치해야 함
	 */
	@Min(value = 0, message = "결제 금액은 0 이상이어야 합니다.")
	private int amount;

	/**
	 * 모의 결제 시뮬레이션 결과
	 * - "SUCCESS" 또는 "FAIL" 값만 허용
	 * - 실제 PG 연동 대신 내부 테스트용
	 */
	@NotNull(message = "시뮬레이션 결과는 필수입니다.")
	@Pattern(regexp = "SUCCESS|FAIL", message = "simulate 값은 SUCCESS 또는 FAIL 이어야 합니다.")
	private String simulate;
}