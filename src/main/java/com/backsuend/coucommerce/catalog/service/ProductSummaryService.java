package com.backsuend.coucommerce.catalog.service;

public interface ProductSummaryService {

	//상품조회시 업데이트
	void setViewCount(Long productId);

	//주문완료시 업데이트
	void setOrderCount(Long productId);

	//좋아요 클릭시 업데이트
	void setZimCount(Long productId);

	//리뷰등록시 업데이트
	void ReviewCount(Long productId);

	//리뷰등록시 평균계산
	void AvgReviewScore(Long productId);

}
