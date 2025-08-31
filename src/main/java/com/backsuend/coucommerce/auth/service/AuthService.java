package com.backsuend.coucommerce.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.dto.AuthResponse;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.jwt.JwtProvider;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.member.repository.AddressRepository;
import com.backsuend.coucommerce.member.repository.MemberRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final AddressRepository addressRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final AuthenticationManager authenticationManager;
	private final RefreshTokenService refreshTokenService;
	private final UserDetailsServiceImpl userDetailsService;

	@Transactional
	public AuthResponse register(SignupRequest request) {
		if (memberRepository.findByEmail(request.email()).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.");
		}

		Member newMember = Member.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.name(request.name())
			.phone(request.phone())
			.role(Role.BUYER) // 기본 역할은 BUYER
			.build();
		memberRepository.save(newMember);

		Address newAddress = Address.builder()
			.member(newMember)
			.postalCode(request.postalCode())
			.roadName(request.roadName())
			.detail(request.detail())
			.build();
		addressRepository.save(newAddress);

		// 회원가입 후 바로 로그인 처리하여 토큰 발급
		return login(new LoginRequest(request.email(), request.password()));
	}

	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.email(), request.password())
		);

		UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();
		return generateTokens(userDetails);
	}

	public AuthResponse generateTokens(UserDetailsImpl userDetails) {
		// 현재 시스템은 사용자당 단일 권한을 가정하므로, 첫 번째 권한을 가져와 사용합니다.
		// 향후 다중 권한을 지원하려면 이 로직의 수정이 필요합니다.
		String roleName = userDetails.getAuthorities().stream()
			.findFirst()
			.map(org.springframework.security.core.GrantedAuthority::getAuthority)
			.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "사용자 권한 정보를 찾을 수 없습니다."));

		//String accessToken = jwtProvider.createAccessToken(userDetails.getUsername(),
		//	Role.valueOf(roleName));

		//********* 권한 문제로 수정 / 권한 앞에 ROLE_ 붙음 ******
		String accessToken = jwtProvider.createAccessToken(userDetails.getUsername(),
			Role.from(roleName));
		//****************************************************

		String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), userDetails.getId());

		return new AuthResponse(accessToken, refreshToken);
	}

	@Transactional
	public AuthResponse refreshAccessToken(String requestRefreshToken) {
		// 1. JWT 유효성 검증 (만료 여부, 서명 유효성 등)
		try {
			jwtProvider.validateToken(requestRefreshToken);
		} catch (ExpiredJwtException e) {
			log.warn("Refresh token expired: {}", requestRefreshToken);
			// 만료된 토큰은 Redis에서도 삭제
			refreshTokenService.deleteByToken(requestRefreshToken);
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "리프레시 토큰이 만료되었습니다.");
		} catch (JwtException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "유효하지 않은 리프레시 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.warn("JWT token is null or empty: {}", e.getMessage());
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 유효하지 않습니다.");
		}

		// 2. Redis에서 리프레시 토큰 정보 조회
		RefreshTokenService.RefreshTokenInfo refreshTokenInfo = refreshTokenService.findByToken(requestRefreshToken)
			.orElseThrow(() -> {
				log.warn("Refresh token not found in Redis or already used: {}", requestRefreshToken);
				// Redis에 없는 토큰은 유효하지 않거나 이미 사용된 것으로 간주
				return new BusinessException(ErrorCode.TOKEN_INVALID, "유효하지 않거나 이미 사용된 리프레시 토큰입니다.");
			});

		// 3. 토큰에서 이메일 추출 및 사용자 정보 로드
		String email = jwtProvider.getEmailFromToken(requestRefreshToken);
		UserDetailsImpl userDetails = userDetailsService.getUserDetailsByEmail(email);

		// 4. 토큰 정보와 사용자 정보 일치 여부 확인
		if (!email.equals(refreshTokenInfo.email())) {
			log.warn("Refresh token email mismatch. Token email: {}, User email: {}", refreshTokenInfo.email(), email);
			// 불일치 시 해당 토큰 삭제 (보안 강화)
			refreshTokenService.deleteByToken(requestRefreshToken);
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰의 사용자 정보가 일치하지 않습니다.");
		}

		// 5. 기존 리프레시 토큰 무효화 (토큰 로테이션)
		refreshTokenService.deleteByToken(requestRefreshToken);
		log.info("Old refresh token invalidated for user: {}", email);

		// 6. 새로운 Access Token 및 Refresh Token 발급
		AuthResponse newTokens = generateTokens(userDetails);
		log.info("New access and refresh tokens issued for user: {}", email);

		return newTokens;
	}
}
