package com.backsuend.coucommerce.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.dto.AuthResponse;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.RefreshTokenRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.service.AuthService;
import com.backsuend.coucommerce.auth.service.RefreshTokenService;
import com.backsuend.coucommerce.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "인증/인가 API", description = "사용자 회원가입, 로그인, 토큰 관리")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;

	@Operation(summary = "회원가입", description = "사용자 정보와 주소 정보를 받아 회원가입을 처리합니다. 가입 시 기본 역할은 'BUYER'로 설정되며, 즉시 로그인 처리되어 토큰이 발급됩니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 및 로그인 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CREATED",
					value = "{\"success\":true,\"status\":201,\"message\":\"CREATED\",\"data\":{\"accessToken\":\"...\",\"refreshToken\":\"...\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/auth/register\",\"errors\":{\"email\":\"이메일 형식이 올바르지 않습니다.\",\"password\":\"비밀번호는 8자 이상이어야 합니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 이메일",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CONFLICT",
					value = "{\"success\":false,\"status\":409,\"message\":\"CONFLICT\",\"data\":{\"code\":\"CONFLICT\",\"message\":\"이미 가입된 이메일입니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/register\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "TOO_MANY_REQUESTS",
					value = "{\"success\":false,\"status\":429,\"message\":\"요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"data\":null,\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
				)
			)
		)
	})
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody SignupRequest request) {
		AuthResponse response = authService.register(request);
		return ApiResponse.created(response).toResponseEntity();
	}

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인을 처리하고, Access/Refresh 토큰을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":{\"grantType\":\"Bearer\",\"accessToken\":\"...\",\"refreshToken\":\"...\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/auth/login\",\"errors\":{\"password\":\"비밀번호는 필수 입력 값입니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNAUTHORIZED",
					value = "{\"success\":false,\"status\":401,\"message\":\"UNAUTHORIZED\",\"data\":{\"code\":\"UNAUTHORIZED\",\"message\":\"이메일 또는 비밀번호가 일치하지 않습니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/login\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "TOO_MANY_REQUESTS",
					value = "{\"success\":false,\"status\":429,\"message\":\"요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"data\":null,\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
				)
			)
		)
	})
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request);
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "토큰 재발급", description = "유효한 Refresh 토큰을 사용하여 새로운 Access/Refresh 토큰 쌍을 발급받습니다. 보안 강화를 위해 토큰 로테이션이 적용되어, 사용된 Refresh 토큰은 무효화되고 새로운 Refresh 토큰이 발급됩니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":{\"accessToken\":\"...\",\"refreshToken\":\"...\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "리프레시 토큰 만료 또는 유효하지 않음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "TOKEN_INVALID",
					value = "{\"success\":false,\"status\":401,\"message\":\"TOKEN_INVALID\",\"data\":{\"code\":\"TOKEN_INVALID\",\"message\":\"리프레시 토큰이 유효하지 않습니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/refresh\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				)
			)
		)
	})
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		AuthResponse authResponse = authService.refreshAccessToken(request.refreshToken());
		return ApiResponse.ok(authResponse).toResponseEntity();
	}

	@Operation(summary = "로그아웃", description = "서버에 저장된 Refresh 토큰을 삭제하여 로그아웃 처리합니다. 클라이언트 측에서도 저장된 토큰을 모두 삭제해야 안전하게 로그아웃이 완료됩니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 성공",
			content = @Content(mediaType = "application/json"
			))
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		refreshTokenService.deleteByToken(request.refreshToken());
		return ApiResponse.<Void>noContent().toResponseEntity();
	}
}
