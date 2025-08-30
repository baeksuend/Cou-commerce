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

	@Operation(summary = "회원가입", description = "사용자 정보와 주소 정보를 받아 회원가입을 처리하고, 즉시 로그인하여 토큰을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 및 로그인 성공",
			content = @Content(schema = @Schema(implementation = AuthResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
	})
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody SignupRequest request) {
		AuthResponse response = authService.register(request);
		return ApiResponse.created(response).toResponseEntity();
	}

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인을 처리하고, Access/Refresh 토큰을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
			content = @Content(schema = @Schema(implementation = AuthResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
	})
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request);
		return ApiResponse.ok(response).toResponseEntity();
	}

	@Operation(summary = "토큰 재발급", description = "유효한 Refresh 토큰을 사용하여 새로운 Access/Refresh 토큰을 발급받습니다. (토큰 로테이션)")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "토큰 재발급 성공", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
			description = "리프레시 토큰 만료 또는 유효하지 않음")
	})
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		AuthResponse authResponse = authService.refreshAccessToken(request.refreshToken());
		return ApiResponse.ok(authResponse).toResponseEntity();
	}

	@Operation(summary = "로그아웃", description = "서버에 저장된 Refresh 토큰을 삭제하여 로그아웃 처리합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 성공")
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		refreshTokenService.deleteByToken(request.refreshToken());
		return ApiResponse.<Void>noContent().toResponseEntity();
	}
}
