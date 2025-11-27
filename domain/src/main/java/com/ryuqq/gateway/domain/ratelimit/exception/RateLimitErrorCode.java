package com.ryuqq.gateway.domain.ratelimit.exception;

import com.ryuqq.gateway.domain.common.exception.ErrorCode;

/**
 * RateLimitErrorCode - Rate Limiting Bounded Context 에러 코드
 *
 * <p>Rate Limiting 도메인에서 발생하는 모든 비즈니스 예외의 에러 코드를 정의합니다.
 *
 * <p><strong>에러 코드 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 형식: RATE-{3자리 숫자}
 *   <li>✅ HTTP 상태 코드 매핑
 *   <li>✅ 명확한 에러 메시지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum RateLimitErrorCode implements ErrorCode {

    /**
     * Rate Limit 초과
     *
     * <p>요청 빈도가 정책을 초과한 경우 발생
     */
    RATE_LIMIT_EXCEEDED("RATE-001", 429, "Too many requests. Please try again later."),

    /**
     * IP 차단됨
     *
     * <p>악의적인 요청 패턴 감지로 IP가 차단된 경우 발생
     */
    IP_BLOCKED("RATE-002", 403, "IP blocked due to abuse. Please try again later."),

    /**
     * 계정 잠금됨
     *
     * <p>너무 많은 실패로 계정이 잠긴 경우 발생
     */
    ACCOUNT_LOCKED("RATE-003", 403, "Account locked due to too many failures.");

    private final String code;
    private final int httpStatus;
    private final String message;

    RateLimitErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
