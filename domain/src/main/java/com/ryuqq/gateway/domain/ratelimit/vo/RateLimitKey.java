package com.ryuqq.gateway.domain.ratelimit.vo;

import java.util.Objects;

/**
 * RateLimitKey - Rate Limit Redis Key Value Object
 *
 * <p>Rate Limiting에 사용되는 Redis Key를 나타내는 불변 객체입니다.
 *
 * <p><strong>Key 형식:</strong>
 *
 * <ul>
 *   <li>ENDPOINT: gateway:rate_limit:endpoint:{path}:{method}
 *   <li>USER: gateway:rate_limit:user:{userId}
 *   <li>IP: gateway:rate_limit:ip:{ipAddress}
 *   <li>OTP: gateway:rate_limit:otp:{phoneNumber}
 *   <li>LOGIN: gateway:rate_limit:login:{ipAddress}
 *   <li>TOKEN_REFRESH: gateway:rate_limit:token_refresh:{userId}
 *   <li>INVALID_JWT: gateway:rate_limit:invalid_jwt:{ipAddress}
 * </ul>
 *
 * @param value Redis Key 문자열
 * @author development-team
 * @since 1.0.0
 */
public record RateLimitKey(String value) {

    /** Compact Constructor (검증 로직) */
    public RateLimitKey {
        Objects.requireNonNull(value, "key cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("key cannot be blank");
        }
    }

    /**
     * 문자열로 RateLimitKey 생성
     *
     * @param key Redis Key 문자열
     * @return RateLimitKey 인스턴스
     * @throws NullPointerException key가 null인 경우
     * @throws IllegalArgumentException key가 blank인 경우
     */
    public static RateLimitKey of(String key) {
        return new RateLimitKey(key);
    }

    /**
     * LimitType과 key 부분들로 RateLimitKey 생성
     *
     * @param limitType 제한 타입
     * @param keyParts 키 구성 요소들
     * @return RateLimitKey 인스턴스
     */
    public static RateLimitKey of(LimitType limitType, String... keyParts) {
        Objects.requireNonNull(limitType, "limitType cannot be null");
        String key = limitType.buildKey(keyParts);
        return new RateLimitKey(key);
    }

    @Override
    public String toString() {
        return "RateLimitKey{" + "value='" + value + '\'' + '}';
    }
}
