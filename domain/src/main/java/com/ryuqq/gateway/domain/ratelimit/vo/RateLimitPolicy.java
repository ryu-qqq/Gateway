package com.ryuqq.gateway.domain.ratelimit.vo;

import java.time.Duration;
import java.util.Objects;

/**
 * RateLimitPolicy - Rate Limit 정책 Value Object
 *
 * <p>Rate Limiting 정책을 나타내는 불변 객체입니다.
 *
 * <p><strong>정책 구성 요소:</strong>
 *
 * <ul>
 *   <li>limitType: 제한 타입 (ENDPOINT, USER, IP, OTP 등)
 *   <li>maxRequests: 최대 허용 요청 수
 *   <li>window: 시간 창 (Duration)
 *   <li>action: 초과 시 수행할 조치
 *   <li>auditLogRequired: Audit Log 필수 여부
 * </ul>
 *
 * @param limitType 제한 타입
 * @param maxRequests 최대 요청 수
 * @param window 시간 창
 * @param action 초과 시 조치
 * @param auditLogRequired Audit Log 필수 여부
 * @author development-team
 * @since 1.0.0
 */
public record RateLimitPolicy(
        LimitType limitType,
        int maxRequests,
        Duration window,
        RateLimitAction action,
        boolean auditLogRequired) {

    /** Compact Constructor (검증 로직) */
    public RateLimitPolicy {
        Objects.requireNonNull(limitType, "limitType cannot be null");
        Objects.requireNonNull(window, "window cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
    }

    /**
     * Rate Limit 정책 생성
     *
     * @param limitType 제한 타입
     * @param maxRequests 최대 요청 수
     * @param window 시간 창
     * @param action 초과 시 조치
     * @param auditLogRequired Audit Log 필수 여부
     * @return RateLimitPolicy 인스턴스
     * @throws NullPointerException limitType, window, action이 null인 경우
     * @throws IllegalArgumentException maxRequests <= 0 또는 window <= 0인 경우
     */
    public static RateLimitPolicy of(
            LimitType limitType,
            int maxRequests,
            Duration window,
            RateLimitAction action,
            boolean auditLogRequired) {
        return new RateLimitPolicy(limitType, maxRequests, window, action, auditLogRequired);
    }

    /**
     * LimitType의 기본 정책 생성
     *
     * <p>각 LimitType에 정의된 기본값을 사용하여 정책을 생성합니다.
     *
     * @param limitType 제한 타입
     * @return 기본 RateLimitPolicy 인스턴스
     */
    public static RateLimitPolicy defaultPolicy(LimitType limitType) {
        Objects.requireNonNull(limitType, "limitType cannot be null");

        RateLimitAction defaultAction = determineDefaultAction(limitType);

        return new RateLimitPolicy(
                limitType,
                limitType.getDefaultMaxRequests(),
                limitType.getDefaultWindow(),
                defaultAction,
                limitType.isAuditLogRequired());
    }

    private static RateLimitAction determineDefaultAction(LimitType limitType) {
        return switch (limitType) {
            case LOGIN, INVALID_JWT -> RateLimitAction.BLOCK_IP;
            case TOKEN_REFRESH -> RateLimitAction.REVOKE_TOKEN;
            default -> RateLimitAction.REJECT;
        };
    }

    /**
     * 현재 카운트가 정책을 초과했는지 확인
     *
     * <p>maxRequests에 도달하면 초과로 판단합니다. 예를 들어 maxRequests=10인 경우, 10번째 요청부터 차단됩니다.
     *
     * @param currentCount 현재 요청 수
     * @return 초과 여부
     */
    public boolean isExceeded(long currentCount) {
        return currentCount >= maxRequests;
    }

    /**
     * 남은 요청 수 계산
     *
     * @param currentCount 현재 요청 수
     * @return 남은 요청 수 (최소 0)
     */
    public int calculateRemaining(long currentCount) {
        long remaining = maxRequests - currentCount;
        return remaining < 0 ? 0 : (int) remaining;
    }

    /**
     * 시간 창(Window)을 초(Seconds) 단위로 조회
     *
     * <p>Law of Demeter 준수를 위한 위임 메서드
     *
     * @return 시간 창(초 단위)
     */
    public long windowSeconds() {
        return window.getSeconds();
    }

    @Override
    public String toString() {
        return "RateLimitPolicy{"
                + "limitType="
                + limitType
                + ", maxRequests="
                + maxRequests
                + ", window="
                + window
                + ", action="
                + action
                + ", auditLogRequired="
                + auditLogRequired
                + '}';
    }
}
