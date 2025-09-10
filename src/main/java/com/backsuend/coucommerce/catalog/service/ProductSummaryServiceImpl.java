package com.backsuend.coucommerce.catalog.service;

import org.springframework.stereotype.Service;

import com.backsuend.coucommerce.catalog.repository.ProductSummaryRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ProductSummaryServiceImpl implements ProductSummaryService {

	private final ProductSummaryRepository productSummaryRepository;

	@Override
	public void setViewCount(Long productId) {
		log.info("상품 조회수 증가 요청: productId={}", productId);
		productSummaryRepository.incrementViewCount(productId);
		log.debug("상품 조회수 증가 완료: productId={}", productId);
	}

	@Override
	public void setOrderCount(Long productId, int count) {
		log.info("상품 주문수 증가 요청: productId={}, count={}", productId, count);
		productSummaryRepository.incrementOrderCount(productId, count);
		log.debug("상품 주문수 증가 완료: productId={}, count={}", productId, count);
	}

	//@Override
	//public void setZimCount(Long productId) {
	//log.info("상품 찜하기 증가 요청: productId={}", productId);
	//	productSummaryRepository.incrementLikeCount(productId);
	//log.info("상품 찜하기 증가 완료: productId={}", productId);
	//}

	@Override
	public void setReviewCount(double newScore, Long productId) {
		log.info("상품 리뷰 등록 요청: productId={}, score={}", productId, newScore);
		productSummaryRepository.incrementReviewCount(newScore, productId);
		log.debug("상품 리뷰 등록 반영 완료: productId={}", productId);
	}

	@Override
	public void setReviewCountEdit(double newScore, Long productId) {
		log.info("상품 리뷰 수정 요청: productId={}, newScore={}", productId, newScore);
		productSummaryRepository.incrementReviewCountEdit(newScore, productId);
		log.debug("상품 리뷰 수정 반영 완료: productId={}", productId);
	}

	@Override
	public void setReviewCountDelete(Long productId) {
		log.info("상품 리뷰 삭제 요청: productId={}", productId);
		productSummaryRepository.incrementReviewCountDelete(productId);
		log.debug("상품 리뷰 삭제 반영 완료: productId={}", productId);
	}

}