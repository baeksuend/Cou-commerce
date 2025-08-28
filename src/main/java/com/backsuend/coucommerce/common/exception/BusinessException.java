package com.backsuend.coucommerce.common.exception;

/**
 * @author rua
 */

import java.util.Map;

/**
 * 도메인/애플리케이션 계층에서 throw 하는 런타임 예외의 표준 베이스.
 * - ErrorCode 로 HTTP 상태/코드/디폴트 메시지를 통일
 * - details 로 추가 컨텍스트를 전달(선택)
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.defaultMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.details = null;
	}

	public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
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
