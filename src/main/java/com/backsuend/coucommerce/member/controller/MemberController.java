package com.backsuend.coucommerce.member.controller;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.dto.ApiResponse;
import com.backsuend.coucommerce.member.dto.AddressChangeRequest;
import com.backsuend.coucommerce.member.dto.PasswordChangeRequest;
import com.backsuend.coucommerce.member.dto.UserProfileResponse;
import com.backsuend.coucommerce.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 API", description = "로그인된 사용자의 프로필, 주소, 비밀번호 등 개인 정보를 관리합니다.")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "내 프로필 조회", description = "현재 로그인된 사용자의 프로필(이름, 이메일, 전화번호) 및 주소 정보를 함께 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":{\"name\":\"홍길동\",\"email\":\"test@example.com\",\"phoneNumber\":\"010-1234-5678\",\"address\":{\"zipCode\":\"12345\",\"address\":\"서울특별시 강남구\",\"detailAddress\":\"테헤란로 123\"}},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/members/me\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 주소 정보를 찾을 수 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"사용자 정보를 찾을 수 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/members/me\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			)
		)
	})
	@SecurityRequirement(name = "Authorization")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		UserProfileResponse userProfile = memberService.getUserProfile(userDetails.getId());
		return ApiResponse.ok(userProfile).toResponseEntity();
	}

	@Operation(summary = "내 주소 변경", description = "현재 로그인된 사용자의 기존 주소를 새로운 주소로 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "주소 변경 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NO_CONTENT",
					value = "{\"success\":true,\"status\":204,\"message\":\"NO_CONTENT\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/members/me/address\",\"errors\":{\"zipCode\":\"우편번호는 필수 입력 값입니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/members/me/address\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 주소 정보를 찾을 수 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"사용자 정보를 찾을 수 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/members/me/address\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			)
		)
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
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "비밀번호 변경 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NO_CONTENT",
					value = "{\"success\":true,\"status\":204,\"message\":\"NO_CONTENT\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"현재 비밀번호가 일치하지 않습니다.\",\"traceId\":null,\"path\":\"/api/v1/members/me/password-change\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"traceId\":null,\"path\":\"/api/v1/members/me/password-change\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		)
	})
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/me/password-change")
	public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		memberService.changePassword(userDetails.getId(), request);
		return ApiResponse.<Void>noContent().toResponseEntity();
	}
}