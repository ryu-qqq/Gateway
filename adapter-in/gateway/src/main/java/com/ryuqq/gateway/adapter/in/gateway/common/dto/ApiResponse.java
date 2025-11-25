package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import java.time.Instant;

/**
 * ApiResponse - 표준 API 응답 래퍼 (Gateway 전용)
 *
 * <p>모든 Gateway 응답의 일관된 형식을 제공합니다.
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 성공 응답
 * ApiResponse<UserDto> response = ApiResponse.ofSuccess(userDto);
 *
 * // 에러 응답
 * ErrorInfo error = new ErrorInfo("JWT_EXPIRED", "토큰이 만료되었습니다");
 * ApiResponse<Void> response = ApiResponse.ofFailure(error);
 * }</pre>
 *
 * <p><strong>응답 형식:</strong>
 *
 * <pre>{@code
 * {
 *   "success": true,
 *   "data": { ... },
 *   "error": null,
 *   "timestamp": "2025-11-25T00:00:00Z",
 *   "traceId": "trace-123456"
 * }
 * }</pre>
 *
 * @param <T> 응답 데이터 타입
 * @author development-team
 * @since 1.0.0
 */
public record ApiResponse<T>(
        boolean success, T data, ErrorInfo error, Instant timestamp, String traceId) {

    /**
     * 성공 응답 생성
     *
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     *
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ofSuccess() {
        return ofSuccess(null);
    }

    /**
     * 실패 응답 생성
     *
     * @param error 에러 정보
     * @param <T> 데이터 타입
     * @return 실패 ApiResponse
     */
    public static <T> ApiResponse<T> ofFailure(ErrorInfo error) {
        return new ApiResponse<>(false, null, error, Instant.now(), null);
    }

    /**
     * 실패 응답 생성 (traceId 포함)
     *
     * @param error 에러 정보
     * @param traceId Trace ID
     * @param <T> 데이터 타입
     * @return 실패 ApiResponse
     */
    public static <T> ApiResponse<T> ofFailure(ErrorInfo error, String traceId) {
        return new ApiResponse<>(false, null, error, Instant.now(), traceId);
    }

    /**
     * 실패 응답 생성 (간편 버전)
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param <T> 데이터 타입
     * @return 실패 ApiResponse
     */
    public static <T> ApiResponse<T> ofFailure(String errorCode, String message) {
        return ofFailure(new ErrorInfo(errorCode, message));
    }

    /**
     * 실패 응답 생성 (간편 버전 + traceId)
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param traceId Trace ID
     * @param <T> 데이터 타입
     * @return 실패 ApiResponse
     */
    public static <T> ApiResponse<T> ofFailure(String errorCode, String message, String traceId) {
        return ofFailure(new ErrorInfo(errorCode, message), traceId);
    }
}
