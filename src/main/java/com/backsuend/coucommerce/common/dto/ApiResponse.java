package com.backsuend.coucommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

/**
 * @author rua
 */
@JsonInclude(Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        Instant timestamp
){
    /* ---------- Factory: Success ---------- */

    public static <T> ApiResponse<T> ok(T data) {
        return of(true, HttpStatus.OK, "OK", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return of(true, HttpStatus.CREATED, "CREATED", data);
    }

    /** 데이터 없는 성공 응답(예: DELETE 204 등) */
    public static <T> ApiResponse<T> noContent() {
        return of(true, HttpStatus.NO_CONTENT, "NO_CONTENT", null);
    }

    /* ---------- Factory: Error ---------- */

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return of(false, status, message, null);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return of(false, HttpStatus.valueOf(statusCode), message, null);
    }

    /* ---------- Common Builder ---------- */

    public static <T> ApiResponse<T> of(boolean success, HttpStatus status, String message, T data) {
        return new ApiResponse<>(success, status.value(), message, data, Instant.now());
    }

    /* ---------- ResponseEntity Helper (선택) ---------- */

    /** 컨트롤러에서 바로 반환하고 싶을 때 사용 */
    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        return ResponseEntity.status(this.status).body(this);
    }
}