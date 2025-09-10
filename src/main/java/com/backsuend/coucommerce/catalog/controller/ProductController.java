package com.backsuend.coucommerce.catalog.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.catalog.dto.PageResponse;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductMainDisplay;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.service.ProductService;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "[비회원] 상품 목록조회 API", description = "비회원이 상품목록, 상세조회 내용을 제공합니다.")
@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class ProductController {

	private final ProductService productService;

	//메인 Best - 정렬순서 order by ps.orderCount desc, ps.zimCount desc
	@Operation(summary = "[비회원] 메인 Best진열상 상품 목록", description = "주문수, 찜한수, 리뷰평점으로 기준으로 역순으로 정렬한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/main_best")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getMainBestProducts() {

		log.info("[API] GET /api/v1/products/main_best 호출하기 ");  // 요청 들어옴 기록

		int page = 10;
		Page<ProductResponse> pageList = productService.getProductsMain(ProductMainDisplay.MAIN_BEST, page);
		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, page);

		log.debug("[API] 메인인기상품 main_best 결과 데이터: {}", productResponse); // 상세 데이터 (개발용)

		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	//메인 Good Review - 정렬순서 ps.reviewCount desc,
	@Operation(summary = "[비회원] 메인 Review 많은순 상품 목록", description = "리뷰 많은순으로 정렬합니다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/good_review")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getMainGoodReviewProducts() {

		log.info("[API] GET /api/v1/products/good_review 호출");  // 요청 들어옴 기록

		int page = 10;
		Page<ProductResponse> pageList = productService.getProductsMain(ProductMainDisplay.MAIN_GOOD_REVIEW, page);
		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, page);

		log.debug("[API] 메인 리뷰많은순 good_view 결과 데이터: {}", productResponse); // 상세 데이터 (개발용)

		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(summary = "[비회원] 카테고리별 상품 목록", description = "비회원이 상세내용을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/category/{category}")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsCategory(
		@ModelAttribute ProductItemSearchRequest req, @PathVariable Category category) {

		log.info("[API] GET /api/v1/products/category/{}?page={}&pageSize={}&sort={}&keyword={} 호출",
			category, req.getPage(), req.getPageSize(), req.getSort(), req.getKeyword());

		Page<ProductResponse> pageList = productService.getProductsUser(req, 0L, category);
		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());

		log.debug("[API] category 내용 결과 데이터: {}", productResponse);

		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));

	}

	@Operation(summary = "[비회원] 상품 상세내용", description = "비회원이 상세내용을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세내용조회 성공 값")
	})
	@GetMapping("/products/{productId}")
	public ResponseEntity<ApiResponse<ProductResponse>> getProductsRead(@PathVariable int productId,
		@ModelAttribute ProductItemSearchRequest req) {

		log.info("[API] GET /api/v1/products/{} 호출", productId);

		ProductResponse productResponse = productService.getRead(ProductReadType.USER_READ, productId, 0L);

		log.debug("[API] 상품상세내용 결과 데이터: {}", productResponse);

		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

}
