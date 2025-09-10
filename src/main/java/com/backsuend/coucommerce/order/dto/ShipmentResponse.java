package com.backsuend.coucommerce.order.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class ShipmentResponse {
	@NotNull
	private String trackingNo;
	@NotNull
	private String carrier;
	@NotNull
	private LocalDateTime shippedAt;
}
