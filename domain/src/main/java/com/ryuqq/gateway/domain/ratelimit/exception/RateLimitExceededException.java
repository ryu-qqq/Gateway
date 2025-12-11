package com.ryuqq.gateway.domain.ratelimit.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * RateLimitExceededException - Rate Limit 초과 예외
 *
 * <p>요청 빈도가 정책을 초과한 경우 발생하는 예외입니다.
 *
 * <p><strong>HTTP 응답:</strong> 429 Too Many Requests
 *
 * <p><strong>Response Headers:</strong>
 *
 * <ul>
 *   <li>X-RateLimit-Limit: 최대 허용 요청 수
 *   <li>X-RateLimit-Remaining: 남은 요청 수 (0)
 *   <li>Retry-After: 재시도 가능 시간 (초)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class RateLimitExceededException extends DomainException {

    private static final int DEFAULT_RETRY_AFTER_SECONDS = 60;

    private final int limit;
    private final int remaining;
    private final int retryAfterSeconds;

    /** 기본 생성자 */
    public RateLimitExceededException() {
        super(RateLimitErrorCode.RATE_LIMIT_EXCEEDED);
        this.limit = 0;
        this.remaining = 0;
        this.retryAfterSeconds = DEFAULT_RETRY_AFTER_SECONDS;
    }

    /**
     * 상세 정보를 포함한 생성자
     *
     * @param limit 최대 허용 요청 수
     * @param remaining 남은 요청 수
     * @param retryAfterSeconds 재시도 가능 시간 (초)
     */
    public RateLimitExceededException(int limit, int remaining, int retryAfterSeconds) {
        super(
                RateLimitErrorCode.RATE_LIMIT_EXCEEDED,
                buildDetail(limit, remaining, retryAfterSeconds));
        this.limit = limit;
        this.remaining = remaining;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * 최대 허용 요청 수 반환
     *
     * @return 최대 허용 요청 수
     */
    public int limit() {
        return limit;
    }

    /**
     * 남은 요청 수 반환
     *
     * @return 남은 요청 수
     */
    public int remaining() {
        return remaining;
    }

    /**
     * 재시도 가능 시간 반환 (초)
     *
     * @return 재시도 가능 시간
     */
    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String buildDetail(int limit, int remaining, int retryAfterSeconds) {
        return String.format(
                "limit=%d, remaining=%d, retryAfterSeconds=%d",
                limit, remaining, retryAfterSeconds);
    }
}
