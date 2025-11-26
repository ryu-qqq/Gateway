package com.ryuqq.gateway.application.ratelimit.dto.response;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;

/**
 * Rate Limit 상태 조회 Response DTO
 *
 * <p>특정 키의 Rate Limit 상태를 담는 Response 객체 (Admin 전용)
 *
 * @param limitType Rate Limit 타입
 * @param identifier 식별자
 * @param currentCount 현재 요청 횟수
 * @param limit 최대 허용 요청 수
 * @param remaining 남은 요청 수
 * @param ttlSeconds 남은 TTL (초)
 * @param blocked 차단 여부
 */
public record RateLimitStatusResponse(
        LimitType limitType,
        String identifier,
        long currentCount,
        int limit,
        int remaining,
        long ttlSeconds,
        boolean blocked) {

    /**
     * 상태 Response 생성
     *
     * @param limitType Rate Limit 타입
     * @param identifier 식별자
     * @param currentCount 현재 요청 횟수
     * @param limit 최대 허용 요청 수
     * @param ttlSeconds 남은 TTL (초)
     * @return RateLimitStatusResponse
     */
    public static RateLimitStatusResponse of(
            LimitType limitType, String identifier, long currentCount, int limit, long ttlSeconds) {
        int remaining = Math.max(0, limit - (int) currentCount);
        boolean blocked = currentCount > limit;
        return new RateLimitStatusResponse(
                limitType, identifier, currentCount, limit, remaining, ttlSeconds, blocked);
    }

    /**
     * 존재하지 않는 키 Response 생성
     *
     * @param limitType Rate Limit 타입
     * @param identifier 식별자
     * @param limit 최대 허용 요청 수
     * @return RateLimitStatusResponse (현재 카운트 0)
     */
    public static RateLimitStatusResponse notFound(
            LimitType limitType, String identifier, int limit) {
        return new RateLimitStatusResponse(limitType, identifier, 0, limit, limit, 0, false);
    }
}
