package com.backsuend.coucommerce.admin.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.admin.dto.MemberStatusChangeRequest;
import com.backsuend.coucommerce.admin.dto.SellerApplicationResponse;
import com.backsuend.coucommerce.admin.dto.SellerRejectionRequest;
import com.backsuend.coucommerce.admin.service.AdminService;
import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "[관리자] API", description = "관리자용 기능 모음")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "[관리자] 회원 상태 변경", description = "관리자가 특정 회원의 상태(활성, 잠김, 휴면)를 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "상태 변경 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없음")
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

	@Operation(summary = "[관리자] 판매자 신청 목록 조회", description = "처리 대기중인 판매자 신청 목록을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)")
	})
	@SecurityRequirement(name = "Authorization")
	@GetMapping("/seller-applications")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<SellerApplicationResponse>>> getPendingSellerApplications() {
		List<SellerApplicationResponse> response = adminService.getPendingSellerApplications();
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "[관리자] 판매자 신청 승인", description = "판매자 신청을 승인합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "승인 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 정보를 찾을 수 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 신청")
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/seller-applications/{applicationId}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> approveSellerApplication(
		@PathVariable Long applicationId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		adminService.approveSellerApplication(applicationId, userDetails.getId());
		return ApiResponse.<Void>noContent().toResponseEntity();
	}

	@Operation(summary = "[관리자] 판매자 신청 거절", description = "판매자 신청을 거절합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "거절 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "거절 사유는 필수입니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 정보를 찾을 수 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 신청")
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/seller-applications/{applicationId}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> rejectSellerApplication(
		@PathVariable Long applicationId,
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@Valid @RequestBody SellerRejectionRequest request) {
		adminService.rejectSellerApplication(applicationId, userDetails.getId(), request);
		return ApiResponse.<Void>noContent().toResponseEntity();
	}
}
