package com.ryuqq.gateway.domain.tenant.vo;

import java.time.Duration;

/**
 * AccessTokenTTL - Access Token 만료 시간 Value Object
 *
 * <p>Access Token의 TTL을 나타내는 불변 객체입니다.
 *
 * @param value Access Token 만료 시간 (양수)
 * @author development-team
 * @since 1.0.0
 */
public record AccessTokenTTL(Duration value) {

    private static final Duration DEFAULT_VALUE = Duration.ofMinutes(15);

    public AccessTokenTTL {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("accessTokenTTL must be positive");
        }
    }

    public static AccessTokenTTL of(Duration value) {
        return new AccessTokenTTL(value);
    }

    public static AccessTokenTTL ofSeconds(long seconds) {
        return new AccessTokenTTL(Duration.ofSeconds(seconds));
    }

    public static AccessTokenTTL ofMinutes(long minutes) {
        return new AccessTokenTTL(Duration.ofMinutes(minutes));
    }

    public static AccessTokenTTL defaultValue() {
        return new AccessTokenTTL(DEFAULT_VALUE);
    }

    public long toSeconds() {
        return value.toSeconds();
    }
}
