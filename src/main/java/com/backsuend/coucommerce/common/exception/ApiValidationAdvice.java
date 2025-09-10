package com.backsuend.coucommerce.common.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.backsuend.coucommerce.common.dto.ApiResponse;

/**
 * @author rua
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiValidationAdvice {

	private Map<String, Object> errorBody(String message, List<Map<String, Object>> errors, HttpServletRequest req) {
		Map<String, Object> body = new HashMap<>();
		body.put("message", message);
		if (errors != null && !errors.isEmpty()) {
			body.put("errors", errors);
		}
		// path는 ApiResponse에 이미 포함되거나 필터에서 처리할 수 있으므로 최소 필드만 반환
		return body;
	}

	private List<Map<String, Object>> fieldErrors(List<FieldError> fieldErrors) {
		return fieldErrors.stream().map(fe -> {
			Map<String, Object> m = new HashMap<>();
			m.put("field", fe.getField());
			m.put("message", fe.getDefaultMessage());
			Object rejected = fe.getRejectedValue();
			if (rejected != null)
				m.put("rejectedValue", rejected);
			return m;
		}).collect(Collectors.toList());
	}

	@ExceptionHandler({MethodArgumentNotValidException.class})
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
		HttpServletRequest req) {
		List<Map<String, Object>> errs = fieldErrors(ex.getBindingResult().getFieldErrors());
		ApiResponse<Object> body = ApiResponse.error(HttpStatus.BAD_REQUEST, "요청 값이 유효하지 않습니다.",
			Map.of("errors", errs));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler({BindException.class})
	public ResponseEntity<ApiResponse<Object>> handleBindException(BindException ex,
		HttpServletRequest req) {
		List<Map<String, Object>> errs = fieldErrors(ex.getBindingResult().getFieldErrors());
		ApiResponse<Object> body = ApiResponse.error(HttpStatus.BAD_REQUEST, "요청 바인딩에 실패했습니다.", Map.of("errors", errs));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}
}
