package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tenant Rate Limit Config Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Tenant Rate Limit Config Entity
 *
 * <p>JSON 직렬화/역직렬화 지원
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantRateLimitConfigEntity {

    private final int loginAttemptsPerHour;
    private final int otpRequestsPerHour;

    /** Constructor (Jackson 역직렬화용) */
    @JsonCreator
    public TenantRateLimitConfigEntity(
            @JsonProperty("loginAttemptsPerHour") int loginAttemptsPerHour,
            @JsonProperty("otpRequestsPerHour") int otpRequestsPerHour) {
        this.loginAttemptsPerHour = loginAttemptsPerHour;
        this.otpRequestsPerHour = otpRequestsPerHour;
    }

    // Getters (Jackson 직렬화용)
    public int getLoginAttemptsPerHour() {
        return loginAttemptsPerHour;
    }

    public int getOtpRequestsPerHour() {
        return otpRequestsPerHour;
    }

    @Override
    public String toString() {
        return "TenantRateLimitConfigEntity{"
                + "loginAttemptsPerHour="
                + loginAttemptsPerHour
                + ", otpRequestsPerHour="
                + otpRequestsPerHour
                + '}';
    }
}
