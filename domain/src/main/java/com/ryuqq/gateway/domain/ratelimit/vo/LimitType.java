package com.ryuqq.gateway.domain.ratelimit.vo;

import java.time.Duration;
import java.util.Objects;

/**
 * LimitType - Rate Limit 타입 열거형
 *
 * <p>Rate Limiting 정책의 타입을 정의합니다. 각 타입은 고유한 Redis Key 패턴,
 * 기본 제한 설정, 그리고 Audit Log 필수 여부를 가집니다.
 *
 * <p><strong>타입별 정책:</strong>
 *
 * <ul>
 *   <li>ENDPOINT: 엔드포인트별 제한 (1,000 req/min)
 *   <li>USER: 사용자별 제한 (100 req/min)
 *   <li>IP: IP별 제한 (100 req/min)
 *   <li>OTP: OTP 요청 제한 (3 req/hour) - Audit Log 필수
 *   <li>LOGIN: 로그인 시도 제한 (5 req/5min) - Audit Log 필수
 *   <li>TOKEN_REFRESH: 토큰 재발급 제한 (3 req/min) - Audit Log 필수
 *   <li>INVALID_JWT: 잘못된 JWT 제출 제한 (10 req/5min) - Audit Log 필수
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum LimitType {

    /**
     * 엔드포인트별 Rate Limit
     *
     * <p>특정 API 엔드포인트에 대한 전체 요청 제한
     */
    ENDPOINT("gateway:rate_limit:endpoint", 1000, Duration.ofMinutes(1), false),

    /**
     * 사용자별 Rate Limit
     *
     * <p>인증된 사용자별 요청 제한
     */
    USER("gateway:rate_limit:user", 100, Duration.ofMinutes(1), false),

    /**
     * IP별 Rate Limit
     *
     * <p>클라이언트 IP 주소별 요청 제한
     */
    IP("gateway:rate_limit:ip", 100, Duration.ofMinutes(1), false),

    /**
     * OTP 요청 Rate Limit
     *
     * <p>SMS 폭탄 공격 방지를 위한 OTP 요청 제한
     */
    OTP("gateway:rate_limit:otp", 3, Duration.ofHours(1), true),

    /**
     * 로그인 시도 Rate Limit
     *
     * <p>Brute Force 공격 방지를 위한 로그인 시도 제한
     */
    LOGIN("gateway:rate_limit:login", 5, Duration.ofMinutes(5), true),

    /**
     * Token Refresh Rate Limit
     *
     * <p>Refresh Token 남용 방지를 위한 재발급 제한
     */
    TOKEN_REFRESH("gateway:rate_limit:token_refresh", 3, Duration.ofMinutes(1), true),

    /**
     * 잘못된 JWT 제출 Rate Limit
     *
     * <p>잘못된 JWT 반복 제출 차단
     */
    INVALID_JWT("gateway:rate_limit:invalid_jwt", 10, Duration.ofMinutes(5), true);

    private final String keyPrefix;
    private final int defaultMaxRequests;
    private final Duration defaultWindow;
    private final boolean auditLogRequired;

    LimitType(String keyPrefix, int defaultMaxRequests, Duration defaultWindow,
              boolean auditLogRequired) {
        this.keyPrefix = keyPrefix;
        this.defaultMaxRequests = defaultMaxRequests;
        this.defaultWindow = defaultWindow;
        this.auditLogRequired = auditLogRequired;
    }

    /**
     * Redis Key Prefix 반환
     *
     * @return Key Prefix
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * 기본 최대 요청 수 반환
     *
     * @return 최대 요청 수
     */
    public int getDefaultMaxRequests() {
        return defaultMaxRequests;
    }

    /**
     * 기본 시간 창 반환
     *
     * @return Duration
     */
    public Duration getDefaultWindow() {
        return defaultWindow;
    }

    /**
     * Audit Log 필수 여부 반환
     *
     * @return Audit Log 필수 여부
     */
    public boolean isAuditLogRequired() {
        return auditLogRequired;
    }

    /**
     * Rate Limit Redis Key 생성
     *
     * <p>Key Prefix와 주어진 부분들을 결합하여 완전한 Redis Key를 생성합니다.
     *
     * @param keyParts 키 구성 요소들
     * @return 완전한 Redis Key
     */
    public String buildKey(String... keyParts) {
        Objects.requireNonNull(keyParts, "keyParts cannot be null");
        if (keyParts.length == 0) {
            throw new IllegalArgumentException("keyParts cannot be empty");
        }

        StringBuilder sb = new StringBuilder(keyPrefix);
        for (String part : keyParts) {
            sb.append(':').append(part);
        }
        return sb.toString();
    }
}
