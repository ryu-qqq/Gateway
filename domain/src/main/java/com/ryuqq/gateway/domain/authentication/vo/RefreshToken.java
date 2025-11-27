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
 * @author development-team
 * @since 1.0.0
 */
public final class RefreshToken {

    private static final int MINIMUM_LENGTH = 32;

    private final String value;

    /** Private Constructor (정적 팩토리 메서드 사용) */
    private RefreshToken(String value) {
        this.value = value;
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
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new RefreshTokenInvalidException(
                    "Refresh token cannot be null or blank");
        }

        if (tokenValue.length() < MINIMUM_LENGTH) {
            throw new RefreshTokenInvalidException(
                    "Refresh token must be at least 32 characters");
        }

        return new RefreshToken(tokenValue);
    }

    /**
     * Refresh Token 값 조회
     *
     * <p>토큰 재발급 시 AuthHub 호출에 사용됩니다.
     *
     * @return Refresh Token 원본 문자열
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RefreshToken that = (RefreshToken) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
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
