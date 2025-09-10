package com.backsuend.coucommerce.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.common.service.EmailService;
import com.backsuend.coucommerce.common.service.MdcLogging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupEmailVerificationService {

	private static final String VERIFICATION_CODE_PREFIX = "email_verification:";
	private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(5); // 5분 유효
	private final EmailService emailService;
	private final StringRedisTemplate stringRedisTemplate; // RedisTemplate 주입

	/**
	 * 회원가입 인증 이메일을 발송합니다.
	 * @param member 인증 이메일을 받을 대상 회원
	 * -
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId, memberRole
	 * - memberId: 사용자 이메일 (비동기 메서드이므로 직접 설정)
	 */
	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendVerificationEmail(Member member) {
		try (var ignored = MdcLogging.withContext("memberId", member.getEmail())) {
			// 1. 인증 코드 생성
			String verificationCode = createVerificationCode();

			// 2. Redis에 인증 코드 저장 (이메일을 키로 사용, 5분 유효)
			String key = VERIFICATION_CODE_PREFIX + member.getEmail();
			stringRedisTemplate.opsForValue().set(key, verificationCode, VERIFICATION_CODE_TTL);
			log.info("{}의 인증 코드가 Redis에 키 {}로 저장되었습니다.", member.getEmail(), key);

			// 3. 이메일 발송
			String subject = "[Cou-commerce] 회원가입을 완료하려면 이메일을 인증해주세요.";
			String text = "안녕하세요, " + member.getName() + "님!\n"
				+ "Cou-commerce에 가입해주셔서 감사합니다.\n"
				+ "회원가입을 완료하려면 인증코드를 입력해 이메일을 인증해주세요.\n"
				+ "인증 코드: " + verificationCode;

			emailService.sendEmail(member.getEmail(), subject, text);

			log.info("{}에게 인증 이메일을 발송했습니다.", member.getEmail());
		}
	}

	/**
	 * 6자리 숫자 인증 코드를 생성합니다.
	 * @return 6자리 숫자 문자열
	 */
	private String createVerificationCode() {
		return String.valueOf((int)(Math.random() * 900000) + 100000);
	}
}
