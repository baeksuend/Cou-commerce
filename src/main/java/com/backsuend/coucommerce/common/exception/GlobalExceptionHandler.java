package com.backsuend.coucommerce.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.backsuend.coucommerce.common.dto.ApiResponse;

/**
 * @author rua
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static String safe(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	/* ======= BusinessException ======= */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleBusiness(
		BusinessException ex, HttpServletRequest req) {

		ErrorCode code = ex.errorCode();
		return build(code, ex.getMessage(), ex.details(), req);
	}

	/* ======= Validation (@Valid/@Validated) ======= */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex, HttpServletRequest req) {

		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
			.forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

		return build(ErrorCode.VALIDATION_FAILED, "요청 본문 검증 실패", fieldErrors, req);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleConstraintViolation(
		ConstraintViolationException ex, HttpServletRequest req) {

		Map<String, String> paramErrors = new LinkedHashMap<>();
		for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
			paramErrors.put(v.getPropertyPath().toString(), v.getMessage());
		}
		return build(ErrorCode.VALIDATION_FAILED, "요청 파라미터 검증 실패", paramErrors, req);
	}

	/* ======= HTTP 스펙 관련 ======= */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleMissingParam(
		MissingServletRequestParameterException ex, HttpServletRequest req) {

		Map<String, String> detail = Map.of(ex.getParameterName(), "required");
		return build(ErrorCode.INVALID_INPUT, ex.getMessage(), detail, req);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleNotReadable(
		HttpMessageNotReadableException ex, HttpServletRequest req) {

		return build(ErrorCode.PAYLOAD_MALFORMED, "유효하지 않은 JSON 본문입니다.", null, req);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {

		return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), null, req);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleUnsupportedMediaType(
		HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {

		return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), null, req);
	}

	/* ======= 보안 관련 ======= */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleAccessDenied(
		AccessDeniedException ex, HttpServletRequest req) {

		return build(ErrorCode.ACCESS_DENIED, ex.getMessage(), null, req);
	}

	/* ======= 데이터/리소스 ======= */
	@ExceptionHandler({NoSuchElementException.class})
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleNotFound(
		RuntimeException ex, HttpServletRequest req) {

		return build(ErrorCode.NOT_FOUND, ex.getMessage(), null, req);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleDataIntegrity(
		DataIntegrityViolationException ex, HttpServletRequest req) {

		return build(ErrorCode.DATA_INTEGRITY_VIOLATION, "데이터 제약조건 위반", null, req);
	}

	/* ======= Fallback ======= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleUnknown(
		Exception ex, HttpServletRequest req) {

		return build(ErrorCode.INTERNAL_ERROR, "예상치 못한 오류가 발생했습니다.", null, req);
	}

	/* ======= 공통 빌더 ======= */
	private ResponseEntity<ApiResponse<ApiErrorPayload>> build(
		ErrorCode code, String message, Object errors, HttpServletRequest req) {

		String traceId = safe(MDC.get("traceId"));         // 로깅 필터에서 넣어두면 추적 가능
		String path = req != null ? req.getRequestURI() : null;

		ApiErrorPayload payload = new ApiErrorPayload(
			code.code(),
			message != null ? message : code.defaultMessage(),
			traceId,
			path,
			errors
		);

		// 상위 ApiResponse의 message에는 "표준 에러 코드"를 올려 클라이언트 분기를 단순화
		ApiResponse<ApiErrorPayload> body =
			ApiResponse.of(false, code.status(), code.code(), payload);

		return body.toResponseEntity();
	}
}
