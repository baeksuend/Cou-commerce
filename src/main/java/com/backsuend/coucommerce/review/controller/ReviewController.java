package com.backsuend.coucommerce.review.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.catalog.dto.PageResponse;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;
import com.backsuend.coucommerce.review.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "[구매자] 리뷰 관리 API", description = "구매자 리뷰관리 기능")
@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@Operation(summary = "[리뷰] 사용자 리뷰 목록", description = "상품별 리뷰 목록을 가져오다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@GetMapping("/products/{productId}/reviews")
	public ResponseEntity<ApiResponse<PageResponse<ReviewResponseDto>>> getReviews(@PathVariable long productId,
		@RequestParam(value = "isAsc", defaultValue = "true") boolean isAsc,
		@RequestParam(value = "page", defaultValue = "1") int page
	) {

		log.info("[API] GET /api/v1/products/{}/reviews 목록 호출", productId);  // 요청 들어옴 기록

		Page<ReviewResponseDto> responseDto = reviewService.getReviews(productId, page - 1, isAsc);
		PageResponse<ReviewResponseDto> reviewResponseDto = new PageResponse<>(responseDto, 10);

		log.debug("[API] 리뷰목록 호출 결과 데이터: {}", reviewResponseDto.getTotalElements()); // 상세 데이터 (개발용)

		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"목록조회 성공",
				reviewResponseDto)
			.toResponseEntity();
	}

	@Operation(summary = "[리뷰] 사용자 리뷰 내용조회", description = "제품의 리뷰내용을 조회한다, 대댓글이 아닐 경우 parentId 값은 null 로 입력. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('BUYER') ")
	@GetMapping("/products/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponse<ReviewResponseDto>> readReview(@PathVariable Long productId,
		@PathVariable Long reviewId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[API] GET /api/v1/products/{}/reviews/{} 목록 호출", productId, reviewId);

		ReviewResponseDto responseDto
			= reviewService.readView(productId, reviewId, userDetails.getId());

		log.debug("[API] 리뷰내용 호출 결과 데이터: {}", responseDto.getId());
		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"내용조회 성공",
				responseDto)
			.toResponseEntity();

	}

	@Operation(summary = "[리뷰] 사용자 리뷰 등록", description = "제품의 리뷰를 등록한다, 대댓글이 아닐 경우 parentId 값은 null 로 입력. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "목록조회 성공 값")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('BUYER') ")
	@PostMapping("/products/{productId}/reviews")
	public ResponseEntity<ApiResponse<ReviewResponseDto>> createReview(@PathVariable Long productId,
		@RequestBody ReviewRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[API] POST /api/v1/products/{}/reviews 등록 호출", productId);

		ReviewResponseDto responseDto
			= reviewService.createReview(productId, requestDto, userDetails.getId());    //, userDetails

		log.debug("[API] 리뷰등록 결과 데이터: {}", responseDto.getId());
		return ApiResponse.of(true,
				HttpStatus.valueOf(201),
				"등록 성공",
				responseDto)
			.toResponseEntity();
	}

	@Operation(summary = "[리뷰] 사용자 리뷰 수정", description = "제품의 리뷰글을 수정합니다, 대댓글이 아닐 경우 parentId 값은 null 로 입력. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('BUYER') ")
	@PutMapping("/products/{productId}/reviews/{reviewId}")
	public ResponseEntity<?> updateReview(@PathVariable Long productId, @PathVariable Long reviewId,
		@RequestBody ReviewRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[API] PUT /api/v1/products/{}/reviews/{} 수정 호출", productId, reviewId);

		ReviewResponseDto responseDto
			= reviewService.updateReview(productId, reviewId, requestDto, userDetails.getId());

		log.debug("[API] 리뷰수정 결과 데이터 : {}", responseDto.getId());
		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"수정 성공",
				responseDto)
			.toResponseEntity();
	}

	@Operation(summary = "[리뷰] 사용자 부모리뷰 삭제", description = "제품의 리뷰를 삭제합니다, 대댓글이 있을 경우 댓글 내용이 삭제된 댓글입니다로 변경 됨. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "목록조회 성공 값")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('BUYER') ")
	@DeleteMapping("/products/{productId}/reviews/{reviewId}")
	public ResponseEntity<?> deleteReview(@PathVariable Long productId, @PathVariable Long reviewId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[API] DELETE /api/v1/products/{}/reviews/{} 수정 호출", productId, reviewId);

		reviewService.deleteReview(productId, reviewId, userDetails.getId());

		log.info("[API] 리뷰 삭제 결과  productId ={} 삭제성공", reviewId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "[리뷰] 사용자 자식리뷰 삭제", description = "제품의 자식리뷰를 삭제합니다, 대댓글이 있을 경우 댓글 내용이 삭제된 댓글입니다로 변경 됨.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "목록조회 성공 값")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('BUYER') ")
	@DeleteMapping("/products/{productId}/reviews/{reviewId}/child/{childReviewId}")
	public ResponseEntity<?> deleteChildReview(@PathVariable Long productId,
		@PathVariable Long reviewId, @PathVariable Long childReviewId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[API] DELETE /api/v1/products/{}/reviews/{}/child/{childReviewId}{} 자식리뷰 호출",
			productId, reviewId, childReviewId);

		reviewService.deleteChildReview(productId, reviewId, childReviewId, userDetails.getId());

		log.info("[API] 자식리뷰 삭제 결과  productId ={} 삭제성공", reviewId);
		return ResponseEntity.noContent().build();
	}

}
