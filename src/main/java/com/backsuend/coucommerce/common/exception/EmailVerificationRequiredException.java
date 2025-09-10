package com.backsuend.coucommerce.common.exception;

public class EmailVerificationRequiredException extends BusinessException {
	public EmailVerificationRequiredException() {
		super(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
	}

	public EmailVerificationRequiredException(String message) {
		super(ErrorCode.EMAIL_VERIFICATION_REQUIRED, message);
	}
}
