package com.backsuend.coucommerce.sellerregistration.dto;

import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRegistrationSearchRequest {
	private Integer page;
	private Integer size;
	private String sortBy;
	private String sortDirection;

	private SellerRegistrationStatus status;
	private String storeName;
	private String businessRegistrationNumber;
	private String memberEmail;
	private String memberName;
}
