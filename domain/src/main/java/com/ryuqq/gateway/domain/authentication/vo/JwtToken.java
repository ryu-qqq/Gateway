package com.ryuqq.gateway.domain.authentication.vo;

import java.time.Clock;
import java.time.Instant;

/**
 * JWT Token Value Object
 *
 * <p>JWT 토큰의 생명주기와 만료 검증을 담당하는 불변 값 객체
 *
 * <p><strong>생성 패턴</strong>:
 *
 * <ul>
 *   <li>{@code of(AccessToken, Instant, Instant)} - 값 기반 생성
 * </ul>
 *
 * <p><strong>도메인 규칙</strong>:
 *
 * <ul>
 *   <li>AccessToken은 null일 수 없다
 *   <li>만료 시간(expiresAt)은 null일 수 없다
 *   <li>생성 시간(createdAt)은 null일 수 없다
 * </ul>
 *
 * @param accessToken JWT Access Token (null 불가)
 * @param expiresAt 만료 시간 (null 불가)
 * @param createdAt 생성 시간 (null 불가)
 * @author development-team
 * @since 1.0.0
 */
public record JwtToken(AccessToken accessToken, Instant expiresAt, Instant createdAt) {

    /** Compact Constructor (검증 로직) */
    public JwtToken {
        if (accessToken == null) {
            throw new IllegalArgumentException("AccessToken cannot be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
    }

    /**
     * 값 기반 생성
     *
     * @param accessToken JWT Access Token (null 불가)
     * @param expiresAt 만료 시간 (null 불가)
     * @param createdAt 생성 시간 (null 불가)
     * @return JwtToken
     * @throws IllegalArgumentException 필수 값이 null인 경우
     */
    public static JwtToken of(AccessToken accessToken, Instant expiresAt, Instant createdAt) {
        return new JwtToken(accessToken, expiresAt, createdAt);
    }

    /**
     * 토큰 만료 여부 검증 (시스템 시계 사용)
     *
     * <p>현재 시간(UTC)이 만료 시간을 지났는지 확인합니다.
     *
     * @return 만료되었으면 true, 아니면 false
     * @author development-team
     * @since 1.0.0
     */
    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    /**
     * 토큰 만료 여부 검증 (테스트 가능)
     *
     * <p>지정된 Clock으로 현재 시간을 측정하여 만료 여부를 확인합니다.
     *
     * <p>테스트 시 Clock.fixed()를 사용하여 시간을 제어할 수 있습니다.
     *
     * @param clock 시간 측정에 사용할 Clock (null 불가)
     * @return 만료되었으면 true, 아니면 false
     * @throws IllegalArgumentException clock이 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    public boolean isExpired(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock cannot be null");
        }
        return Instant.now(clock).isAfter(expiresAt);
    }

    /**
     * AccessToken 원시 값 반환 (Law of Demeter)
     *
     * <p>AccessToken의 원시 String 값이 필요한 경우 Getter 체이닝을 피하기 위해 사용합니다.
     *
     * <p>잘못된 사용: {@code jwtToken.accessToken().value()} ❌
     *
     * <p>올바른 사용: {@code jwtToken.getAccessTokenValue()} ✅
     *
     * @return JWT Access Token 문자열
     * @author development-team
     * @since 1.0.0
     */
    public String getAccessTokenValue() {
        return accessToken.getValue();
    }
}
