package com.ryuqq.gateway.domain.tenant.vo;

import java.time.Duration;

/**
 * SessionConfig - 세션 설정 Value Object
 *
 * <p>테넌트별 세션 정책을 나타내는 불변 객체입니다.
 *
 * <p><strong>정책 구성 요소:</strong>
 *
 * <ul>
 *   <li>MaxActiveSessions: 최대 동시 세션 수 (기본: 5)
 *   <li>AccessTokenTTL: Access Token 만료 시간 (기본: 15분)
 *   <li>RefreshTokenTTL: Refresh Token 만료 시간 (기본: 7일)
 * </ul>
 *
 * @param maxActiveSessions 최대 동시 세션 수 VO
 * @param accessTokenTTL Access Token 만료 시간 VO
 * @param refreshTokenTTL Refresh Token 만료 시간 VO
 * @author development-team
 * @since 1.0.0
 */
public record SessionConfig(
        MaxActiveSessions maxActiveSessions,
        AccessTokenTTL accessTokenTTL,
        RefreshTokenTTL refreshTokenTTL) {

    /**
     * SessionConfig 생성
     *
     * @param maxActiveSessions 최대 동시 세션 수 VO
     * @param accessTokenTTL Access Token 만료 시간 VO
     * @param refreshTokenTTL Refresh Token 만료 시간 VO
     * @return SessionConfig 인스턴스
     */
    public static SessionConfig of(
            MaxActiveSessions maxActiveSessions,
            AccessTokenTTL accessTokenTTL,
            RefreshTokenTTL refreshTokenTTL) {
        return new SessionConfig(maxActiveSessions, accessTokenTTL, refreshTokenTTL);
    }

    /**
     * primitive/Duration 값으로 SessionConfig 생성
     *
     * @param maxActiveSessions 최대 동시 세션 수
     * @param accessTokenTTL Access Token 만료 시간
     * @param refreshTokenTTL Refresh Token 만료 시간
     * @return SessionConfig 인스턴스
     */
    public static SessionConfig of(
            int maxActiveSessions, Duration accessTokenTTL, Duration refreshTokenTTL) {
        return new SessionConfig(
                MaxActiveSessions.of(maxActiveSessions),
                AccessTokenTTL.of(accessTokenTTL),
                RefreshTokenTTL.of(refreshTokenTTL));
    }

    /**
     * 기본 SessionConfig 생성
     *
     * @return 기본 SessionConfig 인스턴스
     */
    public static SessionConfig defaultConfig() {
        return new SessionConfig(
                MaxActiveSessions.defaultValue(),
                AccessTokenTTL.defaultValue(),
                RefreshTokenTTL.defaultValue());
    }

    /**
     * 초 단위로 SessionConfig 생성 (Redis 저장/복원용)
     *
     * @param maxActiveSessions 최대 동시 세션 수
     * @param accessTokenTTLSeconds Access Token TTL (초)
     * @param refreshTokenTTLSeconds Refresh Token TTL (초)
     * @return SessionConfig 인스턴스
     */
    public static SessionConfig ofSeconds(
            int maxActiveSessions, long accessTokenTTLSeconds, long refreshTokenTTLSeconds) {
        return new SessionConfig(
                MaxActiveSessions.of(maxActiveSessions),
                AccessTokenTTL.ofSeconds(accessTokenTTLSeconds),
                RefreshTokenTTL.ofSeconds(refreshTokenTTLSeconds));
    }

    /**
     * 새 세션 생성 가능 여부 확인
     *
     * @param currentSessionCount 현재 활성 세션 수
     * @return 생성 가능하면 true
     */
    public boolean canCreateNewSession(int currentSessionCount) {
        return maxActiveSessions.canCreateNewSession(currentSessionCount);
    }

    /**
     * 최대 동시 세션 수 반환 (primitive)
     *
     * @return 최대 동시 세션 수
     */
    public int maxActiveSessionsValue() {
        return maxActiveSessions.value();
    }

    /**
     * Access Token TTL을 초 단위로 반환
     *
     * @return Access Token TTL (초)
     */
    public long accessTokenTTLSeconds() {
        return accessTokenTTL.toSeconds();
    }

    /**
     * Refresh Token TTL을 초 단위로 반환
     *
     * @return Refresh Token TTL (초)
     */
    public long refreshTokenTTLSeconds() {
        return refreshTokenTTL.toSeconds();
    }

    /**
     * Access Token TTL Duration 반환
     *
     * @return Access Token TTL
     */
    public Duration accessTokenTTLDuration() {
        return accessTokenTTL.value();
    }

    /**
     * Refresh Token TTL Duration 반환
     *
     * @return Refresh Token TTL
     */
    public Duration refreshTokenTTLDuration() {
        return refreshTokenTTL.value();
    }

    @Override
    public String toString() {
        return "SessionConfig{"
                + "maxActiveSessions="
                + maxActiveSessions.value()
                + ", accessTokenTTL="
                + accessTokenTTL.value()
                + ", refreshTokenTTL="
                + refreshTokenTTL.value()
                + '}';
    }
}
