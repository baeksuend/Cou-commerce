package com.backsuend.coucommerce.sellerregistration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.backsuend.coucommerce.sellerregistration.dto.SellerRegistrationSearchRequest;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;

public interface SellerRegistrationRepositoryCustom {
	Page<SellerRegistration> search(SellerRegistrationSearchRequest request, Pageable pageable);
}
