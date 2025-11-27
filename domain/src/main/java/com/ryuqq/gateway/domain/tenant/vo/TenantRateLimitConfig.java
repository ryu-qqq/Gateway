package com.ryuqq.gateway.domain.tenant.vo;

import java.util.Objects;

/**
 * TenantRateLimitConfig - 테넌트별 Rate Limit 설정 Value Object
 *
 * <p>테넌트별 Rate Limiting 정책을 나타내는 불변 객체입니다.
 *
 * <p><strong>정책 구성 요소:</strong>
 *
 * <ul>
 *   <li>loginAttemptsPerHour: 시간당 로그인 시도 횟수 (기본: 10)
 *   <li>otpRequestsPerHour: 시간당 OTP 요청 횟수 (기본: 5)
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);
 * boolean withinLimit = config.isLoginAttemptAllowed(currentCount);
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantRateLimitConfig {

    /** 기본 시간당 로그인 시도 횟수 */
    private static final int DEFAULT_LOGIN_ATTEMPTS_PER_HOUR = 10;

    /** 기본 시간당 OTP 요청 횟수 */
    private static final int DEFAULT_OTP_REQUESTS_PER_HOUR = 5;

    private final int loginAttemptsPerHour;
    private final int otpRequestsPerHour;

    private TenantRateLimitConfig(int loginAttemptsPerHour, int otpRequestsPerHour) {
        this.loginAttemptsPerHour = loginAttemptsPerHour;
        this.otpRequestsPerHour = otpRequestsPerHour;
    }

    /**
     * TenantRateLimitConfig 생성
     *
     * @param loginAttemptsPerHour 시간당 로그인 시도 횟수
     * @param otpRequestsPerHour 시간당 OTP 요청 횟수
     * @return TenantRateLimitConfig 인스턴스
     * @throws IllegalArgumentException 유효하지 않은 파라미터인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static TenantRateLimitConfig of(int loginAttemptsPerHour, int otpRequestsPerHour) {
        validatePositive(loginAttemptsPerHour, "loginAttemptsPerHour");
        validatePositive(otpRequestsPerHour, "otpRequestsPerHour");

        return new TenantRateLimitConfig(loginAttemptsPerHour, otpRequestsPerHour);
    }

    /**
     * 기본 TenantRateLimitConfig 생성
     *
     * <p>모든 값에 기본값을 사용합니다.
     *
     * @return 기본 TenantRateLimitConfig 인스턴스
     * @author development-team
     * @since 1.0.0
     */
    public static TenantRateLimitConfig defaultConfig() {
        return new TenantRateLimitConfig(
                DEFAULT_LOGIN_ATTEMPTS_PER_HOUR, DEFAULT_OTP_REQUESTS_PER_HOUR);
    }

    private static void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * 로그인 시도 허용 여부 확인
     *
     * @param currentCount 현재 로그인 시도 횟수
     * @return 허용되면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean isLoginAttemptAllowed(int currentCount) {
        return currentCount < loginAttemptsPerHour;
    }

    /**
     * OTP 요청 허용 여부 확인
     *
     * @param currentCount 현재 OTP 요청 횟수
     * @return 허용되면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean isOtpRequestAllowed(int currentCount) {
        return currentCount < otpRequestsPerHour;
    }

    /**
     * 시간당 로그인 시도 횟수 반환
     *
     * @return 시간당 로그인 시도 횟수
     * @author development-team
     * @since 1.0.0
     */
    public int getLoginAttemptsPerHour() {
        return loginAttemptsPerHour;
    }

    /**
     * 시간당 OTP 요청 횟수 반환
     *
     * @return 시간당 OTP 요청 횟수
     * @author development-team
     * @since 1.0.0
     */
    public int getOtpRequestsPerHour() {
        return otpRequestsPerHour;
    }

    /**
     * 남은 로그인 시도 횟수 계산
     *
     * @param currentCount 현재 로그인 시도 횟수
     * @return 남은 로그인 시도 횟수 (최소 0)
     * @author development-team
     * @since 1.0.0
     */
    public int calculateRemainingLoginAttempts(int currentCount) {
        int remaining = loginAttemptsPerHour - currentCount;
        return Math.max(remaining, 0);
    }

    /**
     * 남은 OTP 요청 횟수 계산
     *
     * @param currentCount 현재 OTP 요청 횟수
     * @return 남은 OTP 요청 횟수 (최소 0)
     * @author development-team
     * @since 1.0.0
     */
    public int calculateRemainingOtpRequests(int currentCount) {
        int remaining = otpRequestsPerHour - currentCount;
        return Math.max(remaining, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantRateLimitConfig that = (TenantRateLimitConfig) o;
        return loginAttemptsPerHour == that.loginAttemptsPerHour
                && otpRequestsPerHour == that.otpRequestsPerHour;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loginAttemptsPerHour, otpRequestsPerHour);
    }

    @Override
    public String toString() {
        return "TenantRateLimitConfig{"
                + "loginAttemptsPerHour="
                + loginAttemptsPerHour
                + ", otpRequestsPerHour="
                + otpRequestsPerHour
                + '}';
    }
}
