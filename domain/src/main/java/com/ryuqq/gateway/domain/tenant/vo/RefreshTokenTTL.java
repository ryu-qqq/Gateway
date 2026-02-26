package com.ryuqq.gateway.domain.tenant.vo;

import java.time.Duration;

/**
 * RefreshTokenTTL - Refresh Token 만료 시간 Value Object
 *
 * <p>Refresh Token의 TTL을 나타내는 불변 객체입니다.
 *
 * @param value Refresh Token 만료 시간 (양수)
 * @author development-team
 * @since 1.0.0
 */
public record RefreshTokenTTL(Duration value) {

    private static final Duration DEFAULT_VALUE = Duration.ofDays(7);

    public RefreshTokenTTL {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("refreshTokenTTL must be positive");
        }
    }

    public static RefreshTokenTTL of(Duration value) {
        return new RefreshTokenTTL(value);
    }

    public static RefreshTokenTTL ofSeconds(long seconds) {
        return new RefreshTokenTTL(Duration.ofSeconds(seconds));
    }

    public static RefreshTokenTTL ofDays(long days) {
        return new RefreshTokenTTL(Duration.ofDays(days));
    }

    public static RefreshTokenTTL defaultValue() {
        return new RefreshTokenTTL(DEFAULT_VALUE);
    }

    public long toSeconds() {
        return value.toSeconds();
    }
}
