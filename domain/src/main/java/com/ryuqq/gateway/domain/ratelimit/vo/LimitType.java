package com.ryuqq.gateway.domain.ratelimit.vo;

import java.time.Duration;
import java.util.Objects;

/**
 * LimitType - Rate Limit 타입 열거형
 *
 * <p>Rate Limiting 정책의 타입을 정의합니다. 각 타입은 고유한 Redis Key 패턴, 기본 제한 설정, 그리고 Audit Log 필수 여부를 가집니다.
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
    ENDPOINT("gateway:rate_limit:endpoint", 1000, Duration.ofMinutes(1), false, "엔드포인트별 제한"),

    /**
     * 사용자별 Rate Limit
     *
     * <p>인증된 사용자별 요청 제한
     */
    USER("gateway:rate_limit:user", 100, Duration.ofMinutes(1), false, "사용자별 제한"),

    /**
     * IP별 Rate Limit
     *
     * <p>클라이언트 IP 주소별 요청 제한
     */
    IP("gateway:rate_limit:ip", 100, Duration.ofMinutes(1), false, "IP별 제한"),

    /**
     * OTP 요청 Rate Limit
     *
     * <p>SMS 폭탄 공격 방지를 위한 OTP 요청 제한
     */
    OTP("gateway:rate_limit:otp", 3, Duration.ofHours(1), true, "OTP 요청 제한"),

    /**
     * 로그인 시도 Rate Limit
     *
     * <p>Brute Force 공격 방지를 위한 로그인 시도 제한
     */
    LOGIN("gateway:rate_limit:login", 5, Duration.ofMinutes(5), true, "로그인 시도 제한"),

    /**
     * Token Refresh Rate Limit
     *
     * <p>Refresh Token 남용 방지를 위한 재발급 제한
     */
    TOKEN_REFRESH("gateway:rate_limit:token_refresh", 3, Duration.ofMinutes(1), true, "토큰 재발급 제한"),

    /**
     * 잘못된 JWT 제출 Rate Limit
     *
     * <p>잘못된 JWT 반복 제출 차단
     */
    INVALID_JWT("gateway:rate_limit:invalid_jwt", 10, Duration.ofMinutes(5), true, "잘못된 JWT 제한");

    private final String keyPrefix;
    private final int defaultMaxRequests;
    private final Duration defaultWindow;
    private final boolean auditLogRequired;
    private final String displayName;

    LimitType(
            String keyPrefix,
            int defaultMaxRequests,
            Duration defaultWindow,
            boolean auditLogRequired,
            String displayName) {
        this.keyPrefix = keyPrefix;
        this.defaultMaxRequests = defaultMaxRequests;
        this.defaultWindow = defaultWindow;
        this.auditLogRequired = auditLogRequired;
        this.displayName = displayName;
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
     * 표시용 이름 반환
     *
     * @return 표시용 이름
     */
    public String displayName() {
        return displayName;
    }

    /**
     * IP 기반 Rate Limit 타입 여부
     *
     * <p>IP, LOGIN, INVALID_JWT 타입은 IP 주소를 식별자로 사용
     *
     * @return IP 기반 타입이면 true
     */
    public boolean isIpBased() {
        return this == IP || this == LOGIN || this == INVALID_JWT;
    }

    /**
     * User 기반 Rate Limit 타입 여부
     *
     * <p>USER, TOKEN_REFRESH 타입은 사용자 ID를 식별자로 사용
     *
     * @return User 기반 타입이면 true
     */
    public boolean isUserBased() {
        return this == USER || this == TOKEN_REFRESH;
    }

    /**
     * 임계값 초과 시 IP 차단이 필요한 타입 여부
     *
     * <p>LOGIN, INVALID_JWT 타입은 임계값 초과 시 IP를 차단
     *
     * @return IP 차단이 필요하면 true
     */
    public boolean requiresIpBlock() {
        return this == LOGIN || this == INVALID_JWT;
    }

    /**
     * 기본 Rate Limit Action 반환
     *
     * <p>타입별 기본 조치:
     *
     * <ul>
     *   <li>LOGIN, INVALID_JWT → BLOCK_IP
     *   <li>TOKEN_REFRESH → REVOKE_TOKEN
     *   <li>그 외 → REJECT
     * </ul>
     *
     * @return 기본 RateLimitAction
     */
    public RateLimitAction getDefaultAction() {
        return switch (this) {
            case LOGIN, INVALID_JWT -> RateLimitAction.BLOCK_IP;
            case TOKEN_REFRESH -> RateLimitAction.REVOKE_TOKEN;
            default -> RateLimitAction.REJECT;
        };
    }

    /**
     * 실패 임계값 반환
     *
     * <p>IP 차단이 필요한 타입별 임계값:
     *
     * <ul>
     *   <li>LOGIN → 5회
     *   <li>INVALID_JWT → 10회
     *   <li>그 외 → 기본 maxRequests
     * </ul>
     *
     * @return 실패 임계값
     */
    public int getFailureThreshold() {
        return switch (this) {
            case LOGIN -> 5;
            case INVALID_JWT -> 10;
            default -> defaultMaxRequests;
        };
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
