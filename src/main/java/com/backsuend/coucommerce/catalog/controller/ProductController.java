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
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.service.ProductServiceImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;

@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class ProductController {

	private final ProductServiceImpl productService;

	@Operation(summary = "[비회원] 상품 목록", description = "비회원이 상품을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
		@ModelAttribute ProductItemSearchRequest req) {

		Page<ProductResponse> pageList = productService.getProducts(ProductListType.USER_LIST_ALL,
			req.getPage(),
			req.getPageSize(),
			req.getSort(),
			req.getSortDir(),
			0L,
			req.getKeyword(),
			req.getCate());
		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(summary = "[비회원] 카테고리별 상품 목록", description = "비회원이 상세내용을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/category/{category}")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsCategory(@PathVariable String category,
		@ModelAttribute ProductItemSearchRequest req) {

		if (category != null) {

		}

		Page<ProductResponse> pageList = productService.getProducts(ProductListType.USER_LIST_CATEGORY,
			req.getPage(),
			req.getPageSize(),
			req.getSort(),
			req.getSortDir(),
			0L,
			req.getKeyword(),
			req.getCate());
		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(summary = "[비회원] 상품 상세내용", description = "비회원이 상세내용을 조회한다. 비진열, 삭제 상품은 제외한다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세내용조회 성공 값")
	})
	@GetMapping("/products/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> getProductsRead(@PathVariable int id,
		@ModelAttribute ProductItemSearchRequest req) {

		ProductResponse productResponse = productService.getRead(ProductReadType.USER_READ, id, 0L);
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));

	}

}