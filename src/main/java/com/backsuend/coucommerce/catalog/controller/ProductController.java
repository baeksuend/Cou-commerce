package com.backsuend.coucommerce.catalog.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.catalog.dto.PageResponse;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.enums.Category;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.enums.ProductSortType;
import com.backsuend.coucommerce.catalog.service.ProductServiceImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "[비회원] 상품 목록조회 API", description = "비회원이 상품목록, 상세조회 내용을 제공합니다.")
@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class ProductController {

	private final ProductServiceImpl productService;

	//인기순 - 정렬순서, setOrderCount, zimCount, reviewCount좋아요, 평점좋은것 기준
	//평점 좋은순 -
	@Operation(summary = "[비회원] 메인진열상 상품 목록", description = "BEST- ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/main")
	public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsMain(
		@RequestParam(name = "sort") ProductSortType sortType) {

		List<ProductResponse> pageList = productService.getProductsMain(sortType);
		return ResponseEntity.ok().body(ApiResponse.ok(pageList));
	}

	@Operation(summary = "[비회원] 전체 상품 목록", description = "비회원이 상품을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
		@ModelAttribute ProductItemSearchRequest req) {

		Page<ProductResponse> pageList = productService.getProducts(ProductListType.USER_LIST_ALL,
			req, 0L, null);

		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(summary = "[비회원] 카테고리별 상품 목록", description = "비회원이 상세내용을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/category/{category}")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsCategory(
		@ModelAttribute ProductItemSearchRequest req, @PathVariable Category category) {
		Page<ProductResponse> pageList = productService.getProducts(ProductListType.USER_LIST_ALL,
			req, 0L, category);

		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(summary = "[비회원] 상품 상세내용", description = "비회원이 상세내용을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세내용조회 성공 값")
	})
	@GetMapping("/products/{productId}")
	public ResponseEntity<ApiResponse<ProductResponse>> getProductsRead(@PathVariable int productId,
		@ModelAttribute ProductItemSearchRequest req) {

		ProductResponse productResponse = productService.getRead(ProductReadType.USER_READ, productId, 0L);
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));

	}

}