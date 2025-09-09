package com.backsuend.coucommerce.payment.dto;

import org.jetbrains.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author rua
 */

@Getter
@Setter
@AllArgsConstructor
public class RefundRequest {
	@NotNull
	private String reason;
}
