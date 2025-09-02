package com.backsuend.coucommerce.common.exception;

import java.util.Map;

public class CustomValidationException extends RuntimeException {

	//public CustomValidationException(String message) {
	//	super(message);
	//}

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public CustomValidationException(ErrorCode errorCode) {
		super(errorCode.defaultMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public CustomValidationException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.details = null;
	}

	public CustomValidationException(ErrorCode errorCode, String message, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.details = details;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public Map<String, Object> details() {
		return details;
	}

}
