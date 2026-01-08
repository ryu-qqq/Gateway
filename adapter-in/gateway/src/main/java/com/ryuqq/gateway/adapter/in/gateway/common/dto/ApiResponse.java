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
 * // 성공 응답 (requestId 외부 주입 - 권장)
 * String requestId = exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
 * ApiResponse<UserDto> response = ApiResponse.ofSuccess(requestId, userDto);
 *
 * // 성공 응답 (requestId 자동 생성 - fallback)
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
     * 성공 응답 생성 (requestId 외부 주입 - 권장)
     *
     * <p>분산 추적을 위해 TraceIdFilter에서 생성된 requestId를 주입받습니다. GatewayErrorResponder의 ProblemDetail과 동일한
     * requestId를 사용하여 trace continuity를 보장합니다.
     *
     * @param requestId 요청 추적 ID (TraceIdFilter에서 생성)
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ofSuccess(String requestId, T data) {
        String effectiveRequestId =
                (requestId != null && !requestId.isBlank())
                        ? requestId
                        : UUID.randomUUID().toString();
        return new ApiResponse<>(effectiveRequestId, data, Instant.now());
    }

    /**
     * 성공 응답 생성 (requestId 자동 생성 - fallback)
     *
     * <p><strong>주의:</strong> 이 메서드는 새로운 UUID를 생성하므로 trace continuity가 보장되지 않습니다. 가능하면 {@link
     * #ofSuccess(String, Object)} 사용을 권장합니다.
     *
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<>(UUID.randomUUID().toString(), data, Instant.now());
    }

    /**
     * 성공 응답 생성 (데이터 없음, requestId 자동 생성 - fallback)
     *
     * <p><strong>주의:</strong> 이 메서드는 새로운 UUID를 생성하므로 trace continuity가 보장되지 않습니다.
     *
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ofSuccess() {
        return new ApiResponse<>(UUID.randomUUID().toString(), null, Instant.now());
    }
}
