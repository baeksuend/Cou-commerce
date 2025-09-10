package com.backsuend.coucommerce.catalog.service;

public interface ProductSummaryService {

	//상품조회시 업데이트
	void setViewCount(Long productId);

	//주문완료시 업데이트
	void setOrderCount(Long productId, int quntity);

	//좋아요 클릭시 업데이트
	//void setZimCount(Long productId);

	//리뷰등록시 업데이트
	void setReviewCount(double newScore, Long productId);

	//리뷰수정시 업데이트
	void setReviewCountEdit(double newScore, Long productId);

	//리뷰등록시 업데이트
	void setReviewCountDelete(Long productId);

}
