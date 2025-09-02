package com.backsuend.coucommerce.common.exception;

import java.util.Map;

public class NotFoundException extends RuntimeException {
	//public HandlerNotFoundException(String message) {
	//	super(message);
	//}

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public NotFoundException(ErrorCode errorCode) {
		super(errorCode.defaultMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public NotFoundException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.details = null;
	}

	public NotFoundException(ErrorCode errorCode, String message, Map<String, Object> details) {
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
