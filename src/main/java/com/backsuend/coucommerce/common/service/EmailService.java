package com.backsuend.coucommerce.common.service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String senderEmail;

	@Value("${spring.mail.sender-name}")
	private String senderName;

	/**
	 * 간단한 텍스트 이메일을 발송합니다.
	 * @param to 수신자 이메일 주소
	 * @param subject 이메일 제목
	 * @param text 이메일 본문
	 */
	public void sendEmail(String to, String subject, String text) {
		/**
		 * 간단한 텍스트 이메일을 발송합니다.
		 * @param to 수신자 이메일 주소
		 * @param subject 이메일 제목
		 * @param text 이메일 본문
		 * -
		 * MDC-CONTEXT:
		 * - 공통 필드: traceId, memberId (이메일), memberRole
		 * - recipientEmail: 이메일 수신자 주소
		 */
		try (var ignored = MdcLogging.withContext("recipientEmail", to)) {
			try {
				MimeMessage message = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

				helper.setFrom(senderEmail, senderName);
				helper.setTo(to);
				helper.setSubject(subject);
				helper.setText(text, false);

				mailSender.send(message);
				log.info("{}에게 이메일 발송 완료 (발신: {} ({})).", to, senderEmail, senderName);
			} catch (Exception e) {
				log.error("{}에게 이메일 발송 실패 (발신: {} ({})).", to, senderEmail, senderName, e);
				throw new IllegalStateException("이메일 발송에 실패했습니다.");
			}
		}
	}
}
