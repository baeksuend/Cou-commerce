package com.backsuend.coucommerce.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backsuend.coucommerce.auth.dto.AuthResponse;
import com.backsuend.coucommerce.auth.dto.EmailVerificationRequest;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.RefreshTokenRequest;
import com.backsuend.coucommerce.auth.dto.ResendVerificationRequest;
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
	public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody SignupRequest request) {
		AuthResponse response = authService.register(request);

		if (response != null) {
			// 이메일 인증 비활성화 시: 토큰과 함께 201 CREATED 응답
			// ApiResponse.created()가 반환하는 ApiResponse<AuthResponse>를 ResponseEntity로 직접 감쌉니다.
			return new ResponseEntity<>(ApiResponse.created(response), org.springframework.http.HttpStatus.CREATED);
		} else {
			// 이메일 인증 활성화 시: 인증 필요 메시지와 함께 200 OK 응답
			// ApiResponse.ok()가 반환하는 ApiResponse<String>을 ResponseEntity로 직접 감쌉니다.
			return new ResponseEntity<>(ApiResponse.ok("회원가입이 요청되었습니다. 이메일을 확인하여 인증을 완료해주세요."), org.springframework.http.HttpStatus.OK);
		}
	}

	@Operation(summary = "이메일 인증 확인", description = "회원가입 시 발송된 인증 코드를 확인하여 이메일 인증을 완료합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 인증 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":\"이메일 인증이 완료되었습니다.\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "INVALID_INPUT",
					value = "{\"success\":false,\"status\":400,\"message\":\"INVALID_INPUT\",\"data\":{\"code\":\"INVALID_INPUT\",\"message\":\"인증 코드가 일치하지 않습니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/verify-email\",\"errors\":null},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인증 코드 찾을 수 없음 또는 만료",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"인증 코드를 찾을 수 없거나 만료되었습니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/verify-email\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 인증된 계정",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CONFLICT",
					value = "{\"success\":false,\"status\":409,\"message\":\"CONFLICT\",\"data\":{\"code\":\"CONFLICT\",\"message\":\"이미 인증되었거나 활성화된 계정입니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/verify-email\",\"errors\":null},\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
				)
			))
	})
	@PostMapping("/verify-email")
	public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
		authService.verifyEmail(request);
		return ApiResponse.ok("이메일 인증이 완료되었습니다.").toResponseEntity();
	}

	@Operation(summary = "이메일 인증 코드 재전송", description = "이메일 인증 대기 중인 사용자에게 인증 코드를 재전송합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 인증 코드 재전송 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":\"이메일 인증 코드가 재전송되었습니다.\",\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "VALIDATION_FAILED",
					value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/auth/resend-verification\",\"errors\":{\"email\":\"이메일 형식이 올바르지 않습니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"message\":\"NOT_FOUND\",\"data\":{\"code\":\"NOT_FOUND\",\"message\":\"사용자를 찾을 수 없습니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/resend-verification\",\"errors\":null},\"timestamp\":\"2025-09-02T10:40:00.543210Z\"}"
				))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 인증 대기 상태가 아님",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "CONFLICT",
					value = "{\"success\":false,\"status\":409,\"message\":\"CONFLICT\",\"data\":{\"code\":\"CONFLICT\",\"message\":\"이메일 인증 대기 상태가 아닙니다.\",\"traceId\":null,\"path\":\"/api/v1/auth/resend-verification\",\"errors\":null},\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
				)))
	})
	@PostMapping("/resend-verification")
	public ResponseEntity<ApiResponse<String>> resendVerification(
		@Valid @RequestBody ResendVerificationRequest request) {
		authService.resendVerificationEmail(request);
		return ApiResponse.ok("이메일 인증 코드가 재전송되었습니다.").toResponseEntity();
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
