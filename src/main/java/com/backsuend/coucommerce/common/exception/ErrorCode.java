package com.backsuend.coucommerce.common.exception;

/**
 * @author rua
 */

import org.springframework.http.HttpStatus;

/**
 * 서비스 전반에서 공통으로 사용하는 표준 오류 코드.
 * - code   : 머신이 읽기 쉬운 식별자 (로그/모니터링/클라이언트 분기)
 * - status : 매핑될 HTTP 상태
 * - message: 디폴트 사람친화 메시지 (상황에 따라 override 가능)
 */
public enum ErrorCode {

	// 400
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값 검증에 실패했습니다."),
	PAYLOAD_MALFORMED(HttpStatus.BAD_REQUEST, "PAYLOAD_MALFORMED", "요청 본문을 읽을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용되지 않은 메서드입니다."),

	// 401 / 403
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "인증 토큰이 만료되었습니다."),
	TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "인증 토큰이 유효하지 않습니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),

	// 404
	NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),

	// 409
	CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청이 현재 리소스 상태와 충돌합니다."),
	DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "데이터 무결성 제약 위반입니다."),

	// 415
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 콘텐츠 타입입니다."),

	// 429
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."),

	// 500
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String code, String defaultMessage) {
		this.status = status;
		this.code = code;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}
