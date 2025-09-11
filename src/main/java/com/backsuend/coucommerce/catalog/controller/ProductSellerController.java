package com.backsuend.coucommerce.catalog.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.dto.PageResponse;
import com.backsuend.coucommerce.catalog.dto.ProductEditRequest;
import com.backsuend.coucommerce.catalog.dto.ProductItemSearchRequest;
import com.backsuend.coucommerce.catalog.dto.ProductRequest;
import com.backsuend.coucommerce.catalog.dto.ProductResponse;
import com.backsuend.coucommerce.catalog.enums.ProductReadType;
import com.backsuend.coucommerce.catalog.service.ProductService;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "[판매자] 상품 관리 API", description = "판매자 상품 목록, 등록, 수정, 삭제 기능을 제공합니다.")
@RequestMapping("/api/v1/seller")
@RestController
@RequiredArgsConstructor
public class ProductSellerController {

	private final ProductService productService;

	@Operation(summary = "[판매자] 상품 목록", description = "판매자가 상품을 상품명, 등록일, 가격, 카테고리 검색할수 있다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN') ")
	@GetMapping("/products")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getSellerProducts(
		@ModelAttribute ProductItemSearchRequest req,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {

		log.info("[판매자] 상품 목록 호출 GET /api/v1/seller/products 호출");

		Member member = Member.builder().id(userDetails.getId()).email(userDetails.getUsername()).build();
		Page<ProductResponse> pageList = productService.getProductsSeller(req, member, null);
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
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
	@GetMapping("/products/{productId}")
	public ResponseEntity<ApiResponse<ProductResponse>> getSellerProductsRead(@PathVariable long productId,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {

		log.info("[판매자] 상품상세내용 호출  GET /api/v1/seller/products/{} 호출", productId);

		long memberId = userDetails.getId();
		ProductResponse productResponse = productService.getRead(ProductReadType.SELLER_READ, productId, memberId);

		log.info("[판매자] 상품상세내용 호출 완료");
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
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
	@PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<ProductResponse>> getSellerCreate(
		@RequestParam(value = "images", required = false) List<MultipartFile> images,
		@Valid @ModelAttribute ProductRequest dto,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {

		log.info("[판매자] 상품등록 요청 POST /api/v1/seller/products 호출");

		if (images == null) {
			images = new ArrayList<>();
		}

		long memberId = userDetails.getId();
		ProductResponse productResponse = productService.getCreate(dto, memberId, images);

		log.info("[판매자] 상품등록 요청 완료");
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
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
	@PutMapping("/products/{productId}")
	public ResponseEntity<ApiResponse<ProductResponse>> getProductsEdit(
		@PathVariable long productId,
		@RequestParam(value = "images", required = false) List<MultipartFile> images,
		@Valid @ModelAttribute ProductEditRequest dto,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {

		log.info("[판매자] 상품수정 요청 PUT /api/v1/seller/products/{} 호출", productId);

		if (images == null) {
			images = new ArrayList<>();
		}

		long memberId = userDetails.getId();
		ProductResponse productResponse = productService.getEdit(productId, memberId, dto, images);

		log.info("[판매자] 상품수정 요청 완료");
		return ResponseEntity.ok().body(ApiResponse.ok(productResponse));
	}

	@Operation(
		summary = "[판매자] 상품 삭제",
		description = "판매작 상품 삭제한다. 삭제시에는 deleteAt 필드에 삭제일 입력으로 처리한다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "상품삭제완료 값"),
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
	@DeleteMapping("/products/{productId}")
	public ResponseEntity<?> getProductsDelete(@PathVariable long productId,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {

		log.info("[판매자] 상품삭제 요청 DELETE /api/v1/seller/products/{} 호출", productId);

		long memberId = userDetails.getId();
		productService.getDelete(productId, memberId);

		log.info("[판매자] 상품삭제 요청 완료");
		return ResponseEntity.noContent().build();
	}

}