package com.backsuend.coucommerce.member.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.member.dto.AddressChangeRequest;
import com.backsuend.coucommerce.member.dto.PasswordChangeRequest;
import com.backsuend.coucommerce.member.dto.UserProfileResponse;
import com.backsuend.coucommerce.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "회원 API", description = "회원 프로필 관리, 주소 변경 등")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "내 프로필 조회", description = "현재 로그인된 사용자의 상세 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "프로필 조회 성공",
			content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
			description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
			description = "사용자 또는 주소 정보를 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		UserProfileResponse userProfile = memberService.getUserProfile(userDetails.getId());
		return ApiResponse.ok(userProfile).toResponseEntity();
	}

	@Operation(summary = "내 주소 변경", description = "현재 로그인된 사용자의 주소를 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204",
			description = "주소 변경 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
			description = "입력값 유효성 검증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
			description = "인증되지 않은 사용자"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
			description = "사용자 또는 주소 정보를 찾을 수 없음")
	})
	@SecurityRequirement(name = "Authorization")
	@PutMapping("/me/address")
	public ResponseEntity<ApiResponse<Void>> changeAddress(@Valid @RequestBody AddressChangeRequest request,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		memberService.changeAddress(userDetails.getId(), request);
		return ApiResponse.<Void>noContent().toResponseEntity();
	}

	@Operation(summary = "내 비밀번호 변경", description = "현재 로그인된 사용자의 비밀번호를 변경합니다. 변경 후 모든 기기에서 로그아웃됩니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204",
			description = "비밀번호 변경 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
			description = "현재 비밀번호 불일치 또는 새 비밀번호 유효성 검증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
			description = "인증되지 않은 사용자")
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/me/password-change")
	public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		memberService.changePassword(userDetails.getId(), request);
		return ApiResponse.<Void>noContent().toResponseEntity();
	}
}
