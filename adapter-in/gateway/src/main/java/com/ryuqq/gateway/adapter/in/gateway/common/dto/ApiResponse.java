package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ApiResponse - 표준 성공 API 응답 래퍼 (Gateway 전용)
 *
 * <p>Gateway의 성공 응답에 일관된 형식을 제공합니다. 에러 응답은 RFC 7807 ProblemDetail을 사용합니다.
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 성공 응답
 * ApiResponse<UserDto> response = ApiResponse.ofSuccess(userDto);
 *
 * // 에러 응답 → GatewayErrorResponder 사용 (RFC 7807 ProblemDetail)
 * return errorResponder.unauthorized(exchange, "UNAUTHORIZED", "인증이 필요합니다");
 * }</pre>
 *
 * <p><strong>응답 형식:</strong>
 *
 * <pre>{@code
 * {
 *   "requestId": "550e8400-e29b-41d4-a716-446655440000",
 *   "data": { ... },
 *   "timestamp": "2025-11-25T00:00:00Z"
 * }
 * }</pre>
 *
 * @param <T> 응답 데이터 타입
 * @author development-team
 * @since 1.0.0
 */
public record ApiResponse<T>(String requestId, T data, Instant timestamp) {

    /**
     * 성공 응답 생성
     *
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<>(UUID.randomUUID().toString(), data, Instant.now());
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
}
