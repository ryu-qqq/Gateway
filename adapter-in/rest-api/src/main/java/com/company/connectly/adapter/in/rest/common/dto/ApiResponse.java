package com.company.connectly.adapter.in.rest.common.dto;

import java.time.LocalDateTime;

/**
 * ApiResponse - 표준 API 응답 래퍼
 *
 * <p>모든 REST API 응답의 일관된 형식을 제공합니다.</p>
 *
 * <p><strong>사용 예시:</strong></p>
 * <pre>{@code
 * // 성공 응답
 * ApiResponse<UserDto> response = ApiResponse.success(userDto);
 *
 * // 에러 응답
 * ErrorInfo error = new ErrorInfo("USER_NOT_FOUND", "사용자를 찾을 수 없습니다");
 * ApiResponse<Void> response = ApiResponse.failure(error);
 * }</pre>
 *
 * <p><strong>응답 형식:</strong></p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "data": { ... },
 *   "error": null,
 *   "timestamp": "2025-10-23T10:30:00",
 *   "requestId": "req-123456"
 * }
 * }</pre>
 *
 * @param <T> 응답 데이터 타입
 * @author ryu-qqq
 * @since 2025-10-23
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error,
    LocalDateTime timestamp,
    String requestId
) {

    /**
     * 성공 응답 생성
     *
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<>(
            true,
            data,
            null,
            LocalDateTime.now(),
            generateRequestId()
        );
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     *
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     * @author ryu-qqq
     * @since 2025-10-23
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
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> ApiResponse<T> ofFailure(ErrorInfo error) {
        return new ApiResponse<>(
            false,
            null,
            error,
            LocalDateTime.now(),
            generateRequestId()
        );
    }

    /**
     * 실패 응답 생성 (간편 버전)
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param <T> 데이터 타입
     * @return 실패 ApiResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> ApiResponse<T> ofFailure(String errorCode, String message) {
        return ofFailure(new ErrorInfo(errorCode, message));
    }

    /**
     * Request ID 생성
     *
     * <p>실제 운영 환경에서는 MDC나 분산 추적 시스템의 Trace ID를 사용하는 것이 좋습니다.</p>
     *
     * @return Request ID
     * @author ryu-qqq
     * @since 2025-10-23
     */
    private static String generateRequestId() {
        // TODO: MDC or Distributed Tracing ID 사용 권장
        return "req-" + System.currentTimeMillis();
    }
}
