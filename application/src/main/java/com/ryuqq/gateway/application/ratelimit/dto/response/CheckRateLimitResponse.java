package com.ryuqq.gateway.application.ratelimit.dto.response;

import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;

/**
 * Rate Limit 체크 Response DTO
 *
 * <p>Rate Limit 체크 결과를 담는 불변 Response 객체
 *
 * <p><strong>Response 필드</strong>:
 *
 * <ul>
 *   <li>allowed: 요청 허용 여부
 *   <li>currentCount: 현재 요청 횟수
 *   <li>limit: 최대 허용 요청 수
 *   <li>remaining: 남은 요청 수
 *   <li>retryAfterSeconds: 재시도 가능 시간 (초)
 *   <li>action: Rate Limit 초과 시 동작 (REJECT, BLOCK_IP, LOCK_ACCOUNT, REVOKE_TOKEN)
 * </ul>
 *
 * @param allowed 요청 허용 여부
 * @param currentCount 현재 요청 횟수
 * @param limit 최대 허용 요청 수
 * @param remaining 남은 요청 수
 * @param retryAfterSeconds 재시도 가능 시간 (초, 허용된 경우 0)
 * @param action Rate Limit 초과 시 동작
 */
public record CheckRateLimitResponse(
        boolean allowed,
        long currentCount,
        int limit,
        int remaining,
        int retryAfterSeconds,
        RateLimitAction action) {

    /**
     * 허용 Response 생성
     *
     * @param currentCount 현재 요청 횟수
     * @param limit 최대 허용 요청 수
     * @return CheckRateLimitResponse (allowed=true)
     */
    public static CheckRateLimitResponse allowed(long currentCount, int limit) {
        int remaining = Math.max(0, limit - (int) currentCount);
        return new CheckRateLimitResponse(true, currentCount, limit, remaining, 0, null);
    }

    /**
     * 거부 Response 생성
     *
     * @param currentCount 현재 요청 횟수
     * @param limit 최대 허용 요청 수
     * @param retryAfterSeconds 재시도 가능 시간 (초)
     * @param action Rate Limit 동작
     * @return CheckRateLimitResponse (allowed=false)
     */
    public static CheckRateLimitResponse denied(
            long currentCount, int limit, int retryAfterSeconds, RateLimitAction action) {
        return new CheckRateLimitResponse(false, currentCount, limit, 0, retryAfterSeconds, action);
    }
}
