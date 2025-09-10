package com.backsuend.coucommerce.admin.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.admin.dto.MemberStatusChangeRequest;
import com.backsuend.coucommerce.admin.dto.SellerRegistrationResponse;
import com.backsuend.coucommerce.admin.dto.SellerRejectionRequest;
import com.backsuend.coucommerce.admin.service.AdminService;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.sellerregistration.dto.SellerRegistrationSearchRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "[관리자] API", description = "사용자 계정 관리, 판매자 승인 등 관리자 전용 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "[관리자] 회원 상태 변경", description = "관리자가 특정 회원의 상태(ACTIVE, LOCKED, DORMANT)를 변경합니다. 계정이 잠기거나 휴면 처리될 경우, 보안을 위해 해당 사용자의 모든 세션이 강제 종료됩니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "상태 변경 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NO_CONTENT",
					value = "{\"success\":true,\"status\":204,\"message\":\"NO_CONTENT\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값 또는 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/admin/members/1/status\",\"errors\":{\"newStatus\":\"새로운 상태는 필수 입력 값입니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/members/1/status\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "ACCESS_DENIED",
					value = "{\"success\":false,\"status\":403,\"message\":\"ACCESS_DENIED\",\"data\":{\"code\":\"ACCESS_DENIED\",\"message\":\"접근 권한이 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/members/1/status\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"해당 회원을 찾을 수 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/members/999/status\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			))
	})
	@SecurityRequirement(name = "Authorization")
	@PutMapping("/members/{userId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> changeMemberStatus(
		@Parameter(description = "상태를 변경할 회원의 ID") @PathVariable Long userId,
		@Valid @RequestBody MemberStatusChangeRequest request) {
		adminService.changeMemberStatus(userId, request.newStatus());
		return ApiResponse.<Void>noContent().toResponseEntity();
	}

	@Operation(summary = "[관리자] 판매자 등록 신청 목록 검색 및 페이징 조회", description = "관리자가 다양한 조건으로 판매자 등록 신청 목록을 검색하고 페이징하여 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":{\"content\":[{\"registrationId\":1,\"userEmail\":\"seller@example.com\",\"userName\":\"김상인\",\"storeName\":\"My Awesome Store\",\"businessRegistrationNumber\":\"123-45-67890\",\"status\":\"APPLIED\",\"createdAt\":\"2025-09-01T14:00:00\"}],\"pageable\":{\"pageNumber\":0,\"pageSize\":10,\"sort\":{\"empty\":false,\"sorted\":true,\"unsorted\":false},\"offset\":0,\"paged\":true,\"unpaged\":false},\"last\":true,\"totalPages\":1,\"totalElements\":1,\"size\":10,\"number\":0,\"sort\":{\"empty\":false,\"sorted\":true,\"unsorted\":false},\"first\":true,\"numberOfElements\":1,\"empty\":false},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "BAD_REQUEST",
					value = "{\"success\":false,\"status\":400,\"message\":\"BAD_REQUEST\",\"data\":{\"code\":\"BAD_REQUEST\",\"message\":\"잘못된 요청 파라미터입니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "ACCESS_DENIED",
					value = "{\"success\":false,\"status\":403,\"message\":\"ACCESS_DENIED\",\"data\":{\"code\":\"ACCESS_DENIED\",\"message\":\"접근 권한이 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			))
	})
	@SecurityRequirement(name = "Authorization")
	@GetMapping("/seller-registrations")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<SellerRegistrationResponse>>> searchSellerRegistrations(
		@Parameter(description = "판매자 등록 신청 검색 조건") @ModelAttribute SellerRegistrationSearchRequest request,
		@Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
		Page<SellerRegistrationResponse> response = adminService.searchSellerRegistrations(request, pageable);
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "[관리자] 판매자 등록 승인", description = "판매자 신청을 승인하고, 해당 회원의 역할을 'SELLER'로 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "승인 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NO_CONTENT",
					value = "{\"success\":true,\"status\":204,\"message\":\"NO_CONTENT\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/approve\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "ACCESS_DENIED",
					value = "{\"success\":false,\"status\":403,\"message\":\"ACCESS_DENIED\",\"data\":{\"code\":\"ACCESS_DENIED\",\"message\":\"접근 권한이 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/approve\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 정보를 찾을 수 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"해당 신청 정보를 찾을 수 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/999/approve\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 신청",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CONFLICT",
					value = "{\"success\":false,\"status\":409,\"message\":\"CONFLICT\",\"data\":{\"code\":\"CONFLICT\",\"message\":\"이미 처리된 신청입니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/approve\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			))
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/seller-registrations/{registrationId}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> approveSellerRegistration(
		@PathVariable Long registrationId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		adminService.approveSellerRegistration(registrationId, userDetails.getId());
		return ApiResponse.<Void>noContent().toResponseEntity();
	}

	@Operation(summary = "[관리자] 판매자 등록 거절", description = "판매자 신청을 거절하고, 거절 사유를 기록합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "거절 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NO_CONTENT",
					value = "{\"success\":true,\"status\":204,\"message\":\"NO_CONTENT\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "거절 사유는 필수입니다.",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/reject\",\"errors\":{\"rejectionReason\":\"거절 사유는 필수 입력 값입니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/reject\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "ACCESS_DENIED",
					value = "{\"success\":false,\"status\":403,\"message\":\"ACCESS_DENIED\",\"data\":{\"code\":\"ACCESS_DENIED\",\"message\":\"접근 권한이 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/reject\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 정보를 찾을 수 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"해당 신청 정보를 찾을 수 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/999/reject\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 신청",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CONFLICT",
					value = "{\"success\":false,\"status\":409,\"message\":\"CONFLICT\",\"data\":{\"code\":\"CONFLICT\",\"message\":\"이미 처리된 신청입니다.\",\"traceId\":null,\"path\":\"/api/v1/admin/seller-registrations/1/reject\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			))
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/seller-registrations/{registrationId}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> rejectSellerRegistration(
		@PathVariable Long registrationId,
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@Valid @RequestBody SellerRejectionRequest request) {
		adminService.rejectSellerRegistration(registrationId, userDetails.getId(), request);
		return ApiResponse.<Void>noContent().toResponseEntity();
	}
}
