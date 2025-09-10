package com.backsuend.coucommerce.order.dto;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author rua
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipOrderRequest {
	@NotNull
	private String trackingNo;

	@NotNull
	private String carrier;
}
