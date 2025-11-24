package com.ryuqq.gateway.domain.authentication.vo;

import java.time.Instant;

/**
 * JWT Token Value Object
 *
 * <p>JWT 토큰의 생명주기와 만료 검증을 담당하는 불변 값 객체</p>
 *
 * <p><strong>생성 패턴</strong>:</p>
 * <ul>
 *   <li>{@code of(AccessToken, Instant, Instant)} - 값 기반 생성</li>
 * </ul>
 *
 * <p><strong>도메인 규칙</strong>:</p>
 * <ul>
 *   <li>AccessToken은 null일 수 없다</li>
 *   <li>만료 시간(expiresAt)은 null일 수 없다</li>
 *   <li>생성 시간(createdAt)은 null일 수 없다</li>
 * </ul>
 *
 * @param accessToken JWT Access Token (null 불가)
 * @param expiresAt 만료 시간 (null 불가)
 * @param createdAt 생성 시간 (null 불가)
 * @author development-team
 * @since 1.0.0
 */
public record JwtToken(
        AccessToken accessToken,
        Instant expiresAt,
        Instant createdAt
) {

    /**
     * Compact Constructor (검증 로직)
     */
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
     * 토큰 만료 여부 검증
     *
     * <p>현재 시간이 만료 시간을 지났는지 확인합니다.</p>
     *
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
