package com.ryuqq.gateway.domain.authentication.vo;

import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenInvalidException;

/**
 * RefreshToken Value Object
 *
 * <p>Refresh Token 문자열을 검증하고 캡슐화하는 불변 객체
 *
 * <p><strong>도메인 규칙</strong>:
 *
 * <ul>
 *   <li>Refresh Token은 null이나 빈 문자열이 될 수 없다
 *   <li>Refresh Token은 최소 32자 이상이어야 한다
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Refresh Token 문자열 검증
 *   <li>Refresh Token 값 제공 (토큰 재발급용)
 *   <li>보안을 위해 toString에 토큰 값 마스킹
 * </ul>
 *
 * @param value Refresh Token 문자열
 * @author development-team
 * @since 1.0.0
 */
public record RefreshToken(String value) {

    private static final int MINIMUM_LENGTH = 32;

    /** Compact Constructor (검증 로직) */
    public RefreshToken {
        if (value == null || value.isBlank()) {
            throw new RefreshTokenInvalidException("Refresh token cannot be null or blank");
        }

        if (value.length() < MINIMUM_LENGTH) {
            throw new RefreshTokenInvalidException("Refresh token must be at least 32 characters");
        }
    }

    /**
     * Refresh Token 생성
     *
     * <p>Refresh Token 문자열을 검증하여 RefreshToken을 생성합니다.
     *
     * @param tokenValue Refresh Token 문자열
     * @return RefreshToken
     * @throws RefreshTokenInvalidException 토큰이 유효하지 않은 경우
     */
    public static RefreshToken of(String tokenValue) {
        return new RefreshToken(tokenValue);
    }

    /**
     * toString - 보안을 위해 토큰 값 마스킹
     *
     * @return 마스킹된 토큰 정보
     */
    @Override
    public String toString() {
        return "RefreshToken{value='[masked]'}";
    }
}
