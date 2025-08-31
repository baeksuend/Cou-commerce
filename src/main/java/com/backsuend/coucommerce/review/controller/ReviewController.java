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
import com.backsuend.coucommerce.review.dto.ReviewEditRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewRequestDto;
import com.backsuend.coucommerce.review.dto.ReviewResponseDto;
import com.backsuend.coucommerce.review.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@Operation(summary = "[리뷰] 사용자 리뷰 목록", description = "상품별 리뷰 목록을 가져오다. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
	})
	@GetMapping("/products/{product_id}/reviews")
	public ResponseEntity<ApiResponse<PageResponse<ReviewResponseDto>>> getReviews(@PathVariable long product_id,
		@RequestParam(value = "isAsc", defaultValue = "true") boolean isAsc,
		@RequestParam(value = "page", defaultValue = "1") int page
	) {
		Page<ReviewResponseDto> responseDto = reviewService.getReviews(product_id, page - 1, isAsc);
		PageResponse<ReviewResponseDto> reviewResponseDto = new PageResponse<>(responseDto, 10);
		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"조회성공",
				reviewResponseDto)
			.toResponseEntity();
	}

	@Operation(summary = "[리뷰] 사용자 리뷰 등록", description = "제품의 리뷰를 등록한다, 대댓글이 아닐 경우 parentId 값은 null 로 입력. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_BUYER') ")
	@PostMapping("/products/{product_id}/reviews")
	public ResponseEntity<ApiResponse<ReviewResponseDto>> createReview(@PathVariable Long product_id,
		@RequestBody ReviewRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {

		ReviewResponseDto responseDto
			= reviewService.createReview(product_id, requestDto, userDetails.getId());    //, userDetails

		return ApiResponse.of(true,
				HttpStatus.valueOf(201),
				"등록성공",
				responseDto)
			.toResponseEntity();
	}

	@Operation(summary = "[리뷰] 사용자 리뷰 내용조회", description = "제품의 리뷰내용을 조회한다, 대댓글이 아닐 경우 parentId 값은 null 로 입력. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_BUYER') ")
	@GetMapping("/products/{product_id}/reviews/{review_id}")
	public ResponseEntity<ApiResponse<ReviewResponseDto>> readReview(@PathVariable Long product_id,
		@PathVariable Long review_id,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		ReviewResponseDto responseDto
			= reviewService.readView(product_id, review_id, userDetails.getId());    //, userDetails

		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"조회성공",
				responseDto)
			.toResponseEntity();

	}

	@Operation(summary = "[리뷰] 사용자 리뷰 수정", description = "제품의 리뷰글을 수정합니다, 대댓글이 아닐 경우 parentId 값은 null 로 입력. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_BUYER') ")
	@PutMapping("/products/{product_id}/reviews/{review_id}")
	public ResponseEntity<?> updateReview(@PathVariable Long product_id, @PathVariable Long review_id,
		@RequestBody ReviewEditRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {

		ReviewResponseDto responseDto
			= reviewService.updateReview(product_id, review_id, requestDto, userDetails.getId());

		return ApiResponse.of(true,
				HttpStatus.valueOf(200),
				"조회성공",
				responseDto)
			.toResponseEntity();
	}

	@Operation(summary = "[리뷰] 사용자 부모리뷰 삭제", description = "제품의 리뷰를 삭제합니다, 대댓글이 있을 경우 댓글 내용이 삭제된 댓글입니다로 변경 됨. ")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_BUYER') ")
	@DeleteMapping("/products/{product_id}/reviews/{review_id}")
	public ResponseEntity<?> deleteReview(@PathVariable Long product_id, @PathVariable Long review_id,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		reviewService.deleteReview(product_id, review_id, userDetails.getId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "[리뷰] 사용자 자식리뷰 삭제", description = "제품의 자식리뷰를 삭제합니다, 대댓글이 있을 경우 댓글 내용이 삭제된 댓글입니다로 변경 됨.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록조회 성공 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@PreAuthorize("hasRole('ROLE_BUYER') ")
	@DeleteMapping("/products/{product_id}/reviews/{review_id}/child/{childReviewId}")
	public ResponseEntity<?> deleteChildReview(@PathVariable Long product_id,
		@PathVariable Long review_id, @PathVariable Long childReviewId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		reviewService.deleteChildReview(product_id, review_id, childReviewId, userDetails.getId());
		return ResponseEntity.noContent().build();
	}

}
