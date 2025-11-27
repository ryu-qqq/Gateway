package com.ryuqq.gateway.application.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate Limit Configuration Properties
 *
 * <p>Rate Limiting 설정 (gateway.rate-limit.* 기반)
 *
 * <p><strong>테스트 환경 설정 예시</strong>:
 *
 * <pre>{@code
 * gateway:
 *   rate-limit:
 *     enabled: true
 *     endpoint-limit: 5
 *     ip-limit: 5
 *     window-seconds: 60
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class RateLimitProperties {

    /** Rate Limit 활성화 여부 (기본값: true) */
    private boolean enabled = true;

    /** Endpoint Rate Limit (기본값: LimitType.ENDPOINT의 기본값) */
    private Integer endpointLimit;

    /** IP Rate Limit (기본값: LimitType.IP의 기본값) */
    private Integer ipLimit;

    /** User Rate Limit (기본값: LimitType.USER의 기본값) */
    private Integer userLimit;

    /** Login Rate Limit (기본값: LimitType.LOGIN의 기본값) */
    private Integer loginLimit;

    /** OTP Rate Limit (기본값: LimitType.OTP의 기본값) */
    private Integer otpLimit;

    /** Token Refresh Rate Limit (기본값: LimitType.TOKEN_REFRESH의 기본값) */
    private Integer tokenRefreshLimit;

    /** Invalid JWT Rate Limit (기본값: LimitType.INVALID_JWT의 기본값) */
    private Integer invalidJwtLimit;

    /** 기본 Window (초) - 모든 타입에 적용 (설정 시) */
    private Integer windowSeconds;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getEndpointLimit() {
        return endpointLimit;
    }

    public void setEndpointLimit(Integer endpointLimit) {
        this.endpointLimit = endpointLimit;
    }

    public Integer getIpLimit() {
        return ipLimit;
    }

    public void setIpLimit(Integer ipLimit) {
        this.ipLimit = ipLimit;
    }

    public Integer getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(Integer userLimit) {
        this.userLimit = userLimit;
    }

    public Integer getLoginLimit() {
        return loginLimit;
    }

    public void setLoginLimit(Integer loginLimit) {
        this.loginLimit = loginLimit;
    }

    public Integer getOtpLimit() {
        return otpLimit;
    }

    public void setOtpLimit(Integer otpLimit) {
        this.otpLimit = otpLimit;
    }

    public Integer getTokenRefreshLimit() {
        return tokenRefreshLimit;
    }

    public void setTokenRefreshLimit(Integer tokenRefreshLimit) {
        this.tokenRefreshLimit = tokenRefreshLimit;
    }

    public Integer getInvalidJwtLimit() {
        return invalidJwtLimit;
    }

    public void setInvalidJwtLimit(Integer invalidJwtLimit) {
        this.invalidJwtLimit = invalidJwtLimit;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
