package com.ryuqq.gateway.domain.tenant.vo;

import java.time.Duration;
import java.util.Objects;

/**
 * SessionConfig - 세션 설정 Value Object
 *
 * <p>테넌트별 세션 정책을 나타내는 불변 객체입니다.
 *
 * <p><strong>정책 구성 요소:</strong>
 *
 * <ul>
 *   <li>maxActiveSessions: 최대 동시 세션 수 (기본: 5)
 *   <li>accessTokenTTL: Access Token 만료 시간 (기본: 15분)
 *   <li>refreshTokenTTL: Refresh Token 만료 시간 (기본: 7일)
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
 * boolean canCreateSession = config.canCreateNewSession(currentSessionCount);
 * }</pre>
 *
 * @param maxActiveSessions 최대 동시 세션 수
 * @param accessTokenTTL Access Token 만료 시간
 * @param refreshTokenTTL Refresh Token 만료 시간
 * @author development-team
 * @since 1.0.0
 */
public record SessionConfig(int maxActiveSessions, Duration accessTokenTTL, Duration refreshTokenTTL) {

    /** 기본 최대 동시 세션 수 */
    private static final int DEFAULT_MAX_ACTIVE_SESSIONS = 5;

    /** 기본 Access Token TTL (15분) */
    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    /** 기본 Refresh Token TTL (7일) */
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(7);

    /** Compact Constructor (검증 로직) */
    public SessionConfig {
        if (maxActiveSessions <= 0) {
            throw new IllegalArgumentException("maxActiveSessions must be positive");
        }
        Objects.requireNonNull(accessTokenTTL, "accessTokenTTL cannot be null");
        if (accessTokenTTL.isZero() || accessTokenTTL.isNegative()) {
            throw new IllegalArgumentException("accessTokenTTL must be positive");
        }
        Objects.requireNonNull(refreshTokenTTL, "refreshTokenTTL cannot be null");
        if (refreshTokenTTL.isZero() || refreshTokenTTL.isNegative()) {
            throw new IllegalArgumentException("refreshTokenTTL must be positive");
        }
    }

    /**
     * SessionConfig 생성
     *
     * @param maxActiveSessions 최대 동시 세션 수
     * @param accessTokenTTL Access Token 만료 시간
     * @param refreshTokenTTL Refresh Token 만료 시간
     * @return SessionConfig 인스턴스
     * @throws IllegalArgumentException 유효하지 않은 파라미터인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static SessionConfig of(
            int maxActiveSessions, Duration accessTokenTTL, Duration refreshTokenTTL) {
        return new SessionConfig(maxActiveSessions, accessTokenTTL, refreshTokenTTL);
    }

    /**
     * 기본 SessionConfig 생성
     *
     * <p>모든 값에 기본값을 사용합니다.
     *
     * @return 기본 SessionConfig 인스턴스
     * @author development-team
     * @since 1.0.0
     */
    public static SessionConfig defaultConfig() {
        return new SessionConfig(
                DEFAULT_MAX_ACTIVE_SESSIONS, DEFAULT_ACCESS_TOKEN_TTL, DEFAULT_REFRESH_TOKEN_TTL);
    }

    /**
     * 초 단위로 SessionConfig 생성
     *
     * <p>Redis 저장/복원 시 사용합니다.
     *
     * @param maxActiveSessions 최대 동시 세션 수
     * @param accessTokenTTLSeconds Access Token TTL (초)
     * @param refreshTokenTTLSeconds Refresh Token TTL (초)
     * @return SessionConfig 인스턴스
     * @author development-team
     * @since 1.0.0
     */
    public static SessionConfig ofSeconds(
            int maxActiveSessions, long accessTokenTTLSeconds, long refreshTokenTTLSeconds) {
        return of(
                maxActiveSessions,
                Duration.ofSeconds(accessTokenTTLSeconds),
                Duration.ofSeconds(refreshTokenTTLSeconds));
    }

    /**
     * 새 세션 생성 가능 여부 확인
     *
     * @param currentSessionCount 현재 활성 세션 수
     * @return 생성 가능하면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean canCreateNewSession(int currentSessionCount) {
        return currentSessionCount < maxActiveSessions;
    }

    /**
     * Access Token TTL을 초 단위로 반환
     *
     * @return Access Token TTL (초)
     * @author development-team
     * @since 1.0.0
     */
    public long accessTokenTTLSeconds() {
        return accessTokenTTL.toSeconds();
    }

    /**
     * Refresh Token TTL을 초 단위로 반환
     *
     * @return Refresh Token TTL (초)
     * @author development-team
     * @since 1.0.0
     */
    public long refreshTokenTTLSeconds() {
        return refreshTokenTTL.toSeconds();
    }

    @Override
    public String toString() {
        return "SessionConfig{"
                + "maxActiveSessions="
                + maxActiveSessions
                + ", accessTokenTTL="
                + accessTokenTTL
                + ", refreshTokenTTL="
                + refreshTokenTTL
                + '}';
    }
}
