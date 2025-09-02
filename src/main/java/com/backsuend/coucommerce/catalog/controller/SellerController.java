package com.backsuend.coucommerce.catalog.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.dto.PageResponse;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.enums.ProductListType;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.service.ProductService;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "[판매자] 상품 관리 API", description = "판매자 상품 관리 기능")
@RequestMapping("/api/v1/seller")
@RestController
@RequiredArgsConstructor
public class SellerController {

	private final ProductService productService;

	@Operation(summary = "[판매자] 상품 목록", description = "판매자가 상품을 상품명, 등록일, 가격, 카테고리 검색할수 있다. ")
		@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을수 없음")
		})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN') ")
	@GetMapping("/products")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getSellerProducts(
		@ModelAttribute ProductItemSearchRequest req,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		long memberId = userDetails.getId();

		Page<ProductResponse> pageList = productService.getProducts(ProductListType.SELLER_LIST_ALL,
			req.getPage(),
			req.getPageSize(),
			req.getSort(),
			req.getSortDir(),
			memberId,
			req.getKeyword(),
			req.getCate());
		PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());
		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"상품목록 조회 성공",
				productResponse)
			.toResponseEntity();
	}

	@Operation(
		summary = "[판매자] 상품 상세내용",
		description = "판매자가 상품명, 등록일, 상세내용, 가격, 재고수량, 등록일, 카테고리, 진열여부를  조회가능하다. ")
		@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세내용조회 성공 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증하고 권한이 없는 사용자"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을수 없음")
		})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
		@GetMapping("/products/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> getSellerProductsRead(@PathVariable long id,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		long memberId = userDetails.getId();
		ProductResponse productResponse = productService.getRead(ProductReadType.USER_READ, id, memberId);
		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"상품내용 조회 성공",
				productResponse)
			.toResponseEntity();
	}

	@Operation(
		summary = "[판매자] 상품 등록",
		description = "판매자가 상품등록한다.")
		@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "상품 등록완료 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을수 없음")
		})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
	@PostMapping("/products")
	public ResponseEntity<ApiResponse<ProductResponse>> getSellerCreate(@RequestBody @Valid ProductRequest dto,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		long memberId = userDetails.getId();
		ProductResponse productResponse = productService.getCreate(dto, memberId);
		return ApiResponse.of(true,
				HttpStatus.valueOf(201),
				"상품등록 완료",
				productResponse)
			.toResponseEntity();
	}

	@Operation(
		summary = "[판매자] 상품 수정",
		description = "판매자가 상품수정한다. ")
		@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 수정완료 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을수 없음")
		})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
	@PutMapping("/products/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> getProductsEdit(@PathVariable long id,
		@RequestBody @Valid ProductEditRequest dto,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		long memberId = userDetails.getId();
		ProductResponse productResponse = productService.getEdit(id, dto, memberId);
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(
		summary = "[판매자] 상품 삭제",
		description = "판매작 상품 삭제한다. 삭제시에는 deleteAt 필드에 삭제일 입력으로 처리한다.")
		@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "상품삭제완료 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
		})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
	@DeleteMapping("/products/{id}")
	public ResponseEntity<?> getProductsDelete(@PathVariable long id,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		long memberId = userDetails.getId();
		productService.getDelete(id, memberId);
		return ResponseEntity.noContent().build();
	}

}