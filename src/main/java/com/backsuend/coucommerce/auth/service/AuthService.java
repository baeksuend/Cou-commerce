package com.backsuend.coucommerce.auth.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.dto.AuthResponse;
import com.backsuend.coucommerce.auth.dto.EmailVerificationRequest;
import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.ResendVerificationRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.auth.jwt.JwtProvider;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;
import com.backsuend.coucommerce.common.service.MdcLogging;
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

	private static final Logger securityLogger = LoggerFactory.getLogger("securityLogger");

	private static final String VERIFICATION_CODE_PREFIX = "email_verification:";
	private static final String RESEND_LIMIT_PREFIX = "email_resend_daily_limit:";
	private static final int MAX_RESEND_REQUESTS_PER_DAY = 10;

	private final MemberRepository memberRepository;
	private final AddressRepository addressRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final AuthenticationManager authenticationManager;
	private final RefreshTokenService refreshTokenService;
	private final UserDetailsServiceImpl userDetailsService;
	private final SignupEmailVerificationService signupEmailVerificationService;
	private final VerificationAttemptService verificationAttemptService;
	private final StringRedisTemplate stringRedisTemplate;

	@Value("${feature.email-verification.enabled:true}")
	private boolean emailVerificationEnabled;

	@Transactional
	public AuthResponse register(SignupRequest request) {
		Optional<Member> existingMember = memberRepository.findByEmail(request.email());
		if (existingMember.isPresent()) {
			Member member = existingMember.get();
			if (member.getStatus() == MemberStatus.PENDING_VERIFICATION) {
				throw new BusinessException(ErrorCode.EMAIL_ALREADY_PENDING_VERIFICATION);
			} else {
				throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.");
			}
		}

		Member newMember = Member.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.name(request.name())
			.phone(request.phone())
			.role(Role.BUYER)
			.build();

		if (emailVerificationEnabled) {
			newMember.updateStatus(MemberStatus.PENDING_VERIFICATION);
		} else {
			newMember.updateStatus(MemberStatus.ACTIVE);
		}
		memberRepository.save(newMember);

		Address newAddress = Address.builder()
			.member(newMember)
			.postalCode(request.postalCode())
			.roadName(request.roadName())
			.detail(request.detail())
			.build();
		addressRepository.save(newAddress);

		if (emailVerificationEnabled) {
			signupEmailVerificationService.sendVerificationEmail(newMember);
			log.info("이메일 인증이 활성화되었습니다. 사용자 {}의 가입 및 인증 이메일 발송이 완료되었습니다.",
				newMember.getEmail());
			// 토큰 없이 null을 반환하여 컨트롤러에서 인증 필요 상태를 처리하도록 함
			return null;
		} else {
			log.info("이메일 인증이 비활성화되었습니다. {} 계정으로 자동 로그인을 진행합니다.", newMember.getEmail());
			return login(new LoginRequest(request.email(), request.password()));
		}
	}

	@Transactional
	public void verifyEmail(EmailVerificationRequest request) {
		if (verificationAttemptService.isBlocked(request.email())) {
			throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
		}

		String key = VERIFICATION_CODE_PREFIX + request.email();
		String storedCode = stringRedisTemplate.opsForValue().get(key);

		if (storedCode == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "인증 코드를 찾을 수 없거나 만료되었습니다.");
		}

		if (!storedCode.equals(request.verificationCode())) {
			verificationAttemptService.handleFailedAttempt(request.email());
			throw new BusinessException(ErrorCode.INVALID_INPUT, "인증 코드가 일치하지 않습니다.");
		}

		Member member = memberRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		if (member.getStatus() != MemberStatus.PENDING_VERIFICATION) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 인증되었거나 활성화된 계정입니다.");
		}

		member.updateStatus(MemberStatus.ACTIVE);
		memberRepository.save(member);

		stringRedisTemplate.delete(key);
		verificationAttemptService.resetAttempts(request.email());
		log.info("이메일 {}의 인증 및 계정 활성화가 성공적으로 완료되었습니다.", request.email());
	}

	@Transactional
	public void resendVerificationEmail(ResendVerificationRequest request) {
		checkDailyResendLimit(request.email());

		if (verificationAttemptService.isBlocked(request.email())) {
			throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "인증 시도 횟수 초과로 계정이 잠겼습니다. 잠시 후 다시 시도해주세요.");
		}

		String verificationCodeKey = VERIFICATION_CODE_PREFIX + request.email();
		if (stringRedisTemplate.hasKey(verificationCodeKey)) {
			throw new BusinessException(ErrorCode.CONFLICT, "아직 유효한 인증 코드가 존재합니다. 5분 후에 다시 시도해주세요.");
		}

		Member member = memberRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		if (member.getStatus() != MemberStatus.PENDING_VERIFICATION) {
			throw new BusinessException(ErrorCode.CONFLICT, "이메일 인증 대기 상태가 아닙니다.");
		}

		signupEmailVerificationService.sendVerificationEmail(member);
		log.info("{} 주소로 인증 이메일을 재발송했습니다.", request.email());
	}

	private void checkDailyResendLimit(String email) {
		String limitKey = RESEND_LIMIT_PREFIX + email;
		Long currentCount = stringRedisTemplate.opsForValue().increment(limitKey);

		if (currentCount == null) {
			currentCount = 1L;
		}

		if (currentCount == 1) {
			LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
			stringRedisTemplate.expire(limitKey, Duration.between(LocalDateTime.now(), endOfDay));
		}

		if (currentCount > MAX_RESEND_REQUESTS_PER_DAY) {
			throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "하루에 재전송 가능한 횟수를 초과했습니다. 내일 다시 시도해주세요.");
		}
	}

	/**
	 * 로그인을 처리하고, 성공 시 새로운 Access/Refresh 토큰을 발급합니다.
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberRole
	 * - memberId: 사용자 이메일 (로그인 시점에는 인증 전이므로 직접 설정)
	 */
	@Transactional
	public AuthResponse login(LoginRequest request) {
		try (var ignored = MdcLogging.withContext("memberId", request.email())) {
			try {
				Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password())
				);

				// --- 로그인 성공 ---
				log.info("사용자 로그인 성공");
				securityLogger.info("로그인 성공: 사용자 '{}'", request.email());

				UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();

				Member member = memberRepository.findByEmail(userDetails.getUsername())
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

				if (member.getStatus() == MemberStatus.PENDING_VERIFICATION) {
					throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 인증이 완료되지 않았습니다.");
				}

				member.updateLastLoggedInAt();

				return generateTokens(userDetails);

			} catch (AuthenticationException e) {
				// --- 로그인 실패 ---
				log.warn("사용자 로그인 실패: {}", e.getMessage());
				securityLogger.warn("로그인 실패: 사용자 '{}', 원인: {}", request.email(), e.getMessage());
				throw e; // 예외를 다시 던져서 GlobalExceptionHandler 또는 Spring Security에서 처리하도록 함
			}
		}
	}

	public AuthResponse generateTokens(UserDetailsImpl userDetails) {
		// 현재 시스템은 사용자당 단일 권한을 가정하므로, 첫 번째 권한을 가져와 사용합니다.
		// 향후 다중 권한을 지원하려면 이 로직의 수정이 필요합니다.
		String roleName = userDetails.getAuthorities().stream()
			.findFirst()
			.map(GrantedAuthority::getAuthority)
			.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "사용자 권한 정보를 찾을 수 없습니다."));

		// "ROLE_BUYER" -> "BUYER"
		String roleEnumName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;

		String accessToken = jwtProvider.createAccessToken(userDetails.getUsername(),
			Role.valueOf(roleEnumName));
		String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), userDetails.getId());

		return new AuthResponse(accessToken, refreshToken);
	}

	/**
	 * 만료된 Access Token을 새로운 토큰으로 갱신합니다.
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberId (이메일), memberRole
	 * - refreshTokenHint: 리프레시 토큰의 일부 (앞 10자리)
	 */
	@Transactional
	public AuthResponse refreshAccessToken(String requestRefreshToken) {
		try (var ignored = MdcLogging.withContext("refreshTokenHint",
			requestRefreshToken != null && requestRefreshToken.length() > 10 ? requestRefreshToken.substring(0, 10)
				: requestRefreshToken)) {
			// 1. JWT 유효성 검증 (만료 여부, 서명 유효성 등)
			try {
				jwtProvider.validateToken(requestRefreshToken);
			} catch (ExpiredJwtException e) {
				log.warn("리프레시 토큰이 만료되었습니다: {}", requestRefreshToken);
				// 만료된 토큰은 Redis에서도 삭제
				refreshTokenService.deleteByToken(requestRefreshToken);
				throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "리프레시 토큰이 만료되었습니다.");
			} catch (JwtException | IllegalArgumentException e) {
				log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
				throw new BusinessException(ErrorCode.TOKEN_INVALID, "유효하지 않은 리프레시 토큰입니다.");
			}

			// 2. Redis에서 리프레시 토큰 정보 조회
			RefreshTokenService.RefreshTokenInfo refreshTokenInfo = refreshTokenService.findByToken(requestRefreshToken)
				.orElseThrow(() -> {
					log.warn("Redis에서 리프레시 토큰을 찾을 수 없거나 이미 사용된 토큰입니다: {}", requestRefreshToken);
					// Redis에 없는 토큰은 유효하지 않거나 이미 사용된 것으로 간주
					return new BusinessException(ErrorCode.TOKEN_INVALID, "유효하지 않거나 이미 사용된 리프레시 토큰입니다.");
				});

			if (refreshTokenInfo.used()) {
				log.warn("사용자 {}의 리프레시 토큰 재사용이 감지되었습니다. 모든 토큰을 무효화합니다.", refreshTokenInfo.userId());
				refreshTokenService.deleteAllTokensForUser(refreshTokenInfo.userId());
				throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 이미 사용되었습니다. 모든 세션이 종료됩니다.");
			}

			// 3. 토큰에서 이메일 추출 및 사용자 정보 로드
			String email = jwtProvider.getEmailFromToken(requestRefreshToken);
			UserDetailsImpl userDetails = userDetailsService.getUserDetailsByEmail(email);

			// 4. 토큰 정보와 사용자 정보 일치 여부 확인
			if (!email.equals(refreshTokenInfo.email())) {
				log.warn("리프레시 토큰의 이메일이 일치하지 않습니다. 토큰 이메일: {}, 사용자 이메일: {}", refreshTokenInfo.email(), email);
				// 불일치 시 해당 토큰 삭제 (보안 강화)
				refreshTokenService.deleteByToken(requestRefreshToken);
				throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰의 사용자 정보가 일치하지 않습니다.");
			}

			// 5. 기존 리프레시 토큰 무효화 (토큰 로테이션)
			refreshTokenService.deleteByToken(requestRefreshToken);
			log.info("사용자 {}의 기존 리프레시 토큰을 무효화했습니다.", email);

			// 6. 새로운 Access Token 및 Refresh Token 발급
			AuthResponse newTokens = generateTokens(userDetails);
			log.info("사용자 {}에게 새로운 액세스 토큰과 리프레시 토큰을 발급했습니다.", email);

			return newTokens;
		}
	}
}
