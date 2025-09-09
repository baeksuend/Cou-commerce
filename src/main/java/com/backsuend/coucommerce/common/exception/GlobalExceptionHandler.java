package com.backsuend.coucommerce.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

import lombok.extern.slf4j.Slf4j;

/**
 * @author rua
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 문자열이 null이거나 비어있는 경우 null을 반환하고, 그렇지 않으면 원본 문자열을 반환한다.
	 * 주로 MDC(Mapped Diagnostic Context)에서 traceId를 안전하게 가져올 때 사용된다.
	 */
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

	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleAuthentication(
		org.springframework.security.core.AuthenticationException ex, HttpServletRequest req) {

		return build(ErrorCode.UNAUTHORIZED, "인증에 실패했습니다.", null, req);
	}

	@ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleExpiredJwt(
		io.jsonwebtoken.ExpiredJwtException ex, HttpServletRequest req) {

		return build(ErrorCode.TOKEN_EXPIRED, "인증 토큰이 만료되었습니다.", null, req);
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

		// "idx_member_email" 문자열을 포함하는 경우, 이메일 중복으로 간주
		if (ex.getMessage() != null && ex.getMessage().contains("idx_member_email")) {
			return build(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.", null, req);
		}

		return build(ErrorCode.DATA_INTEGRITY_VIOLATION, "데이터 제약조건 위반", null, req);
	}

	/* ======= Fallback ======= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ApiErrorPayload>> handleUnknown(
		Exception ex, HttpServletRequest req) {

		System.out.println("Unhandled exception occurred: " + ex);

		return build(ErrorCode.INTERNAL_ERROR, "예상치 못한 오류가 발생했습니다.====", null, req);
	}

	/* ======= 공통 빌더 ======= */

	/**
	 * 공통 에러 응답 페이로드(ApiErrorPayload)를 생성하고, 이를 ApiResponse로 감싸 ResponseEntity를 반환한다.
	 * MDC에 설정된 traceId와 요청 경로를 포함하여 에러 추적을 용이하게 한다.
	 */
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

	/**
	 * service에서 잘못입력된 부분을 400번 에러로 처리한다.
	 */
	@ExceptionHandler(CustomValidationException.class)
	public ResponseEntity<ApiResponse<Object>> CustomValidationException(CustomValidationException ex,
		HttpServletRequest req) {

		ApiResponse<Object> error = ApiResponse.error(
			HttpStatus.BAD_REQUEST,
			ex.getMessage()
		);

		return ResponseEntity.badRequest().body(error);
	}

	/**
	 * service에서 리소스 찾을수 없을때 404 에러 출력
	 */
	@ExceptionHandler({
		NotFoundException.class})
	public ResponseEntity<ApiResponse<Object>> handlerNotFoundException(NotFoundException ex,
		HttpServletRequest req) {

		ApiResponse<Object> error = ApiResponse.error(
			HttpStatus.NOT_FOUND,
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

	}

}
