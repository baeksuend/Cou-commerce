package com.backsuend.coucommerce.catalog.service;

import org.springframework.stereotype.Service;

import com.backsuend.coucommerce.catalog.repository.ProductSummaryRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductSummaryServiceImpl implements ProductSummaryService {

	private final ProductSummaryRepository productSummaryRepository;

	@Override
	public void setViewCount(Long productId) {
		productSummaryRepository.incrementViewCount(productId);
	}

	@Override
	public void setOrderCount(Long productId) {
		productSummaryRepository.incrementOrderCount(productId);
	}

	@Override
	public void setZimCount(Long productId) {
		productSummaryRepository.incrementLikeCount(productId);
	}

	@Override
	public void ReviewCount(Long productId) {
		productSummaryRepository.incrementReviewCount(productId);
	}

	@Override
	public void AvgReviewScore(Long productId) {
		productSummaryRepository.incrementAveReviewCount(productId);
	}
}
