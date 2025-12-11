package com.ryuqq.gateway.domain.authentication.vo;

/**
 * TokenPair Value Object
 *
 * <p>Access Token + Refresh Token 쌍을 캡슐화하는 불변 객체
 *
 * <p><strong>도메인 규칙</strong>:
 *
 * <ul>
 *   <li>Access Token은 null이 될 수 없다
 *   <li>Refresh Token은 null이 될 수 없다
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Token Refresh 결과물 (새 Access Token + 새 Refresh Token) 표현
 *   <li>Refresh Token Rotation 시 새 토큰 쌍 전달
 *   <li>보안을 위해 toString에 토큰 값 마스킹
 * </ul>
 *
 * @param accessToken Access Token VO
 * @param refreshToken Refresh Token VO
 * @author development-team
 * @since 1.0.0
 */
public record TokenPair(AccessToken accessToken, RefreshToken refreshToken) {

    /** Compact Constructor (검증 로직) */
    public TokenPair {
        if (accessToken == null) {
            throw new IllegalArgumentException("Access token cannot be null");
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token cannot be null");
        }
    }

    /**
     * TokenPair 생성 (VO 객체로)
     *
     * @param accessToken Access Token VO
     * @param refreshToken Refresh Token VO
     * @return TokenPair
     * @throws IllegalArgumentException 토큰이 null인 경우
     */
    public static TokenPair of(AccessToken accessToken, RefreshToken refreshToken) {
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * TokenPair 생성 (문자열로)
     *
     * <p>문자열을 AccessToken, RefreshToken VO로 변환 후 TokenPair 생성
     *
     * @param accessTokenValue Access Token 문자열
     * @param refreshTokenValue Refresh Token 문자열
     * @return TokenPair
     */
    public static TokenPair of(String accessTokenValue, String refreshTokenValue) {
        AccessToken accessToken = AccessToken.of(accessTokenValue);
        RefreshToken refreshToken = RefreshToken.of(refreshTokenValue);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Access Token 값(문자열) 조회
     *
     * <p>Law of Demeter 준수를 위한 위임 메서드
     *
     * @return Access Token 문자열 값
     */
    public String accessTokenValue() {
        return accessToken.value();
    }

    /**
     * Refresh Token 값(문자열) 조회
     *
     * <p>Law of Demeter 준수를 위한 위임 메서드
     *
     * @return Refresh Token 문자열 값
     */
    public String refreshTokenValue() {
        return refreshToken.value();
    }

    /**
     * toString - 보안을 위해 토큰 값 마스킹
     *
     * @return 마스킹된 토큰 정보
     */
    @Override
    public String toString() {
        return "TokenPair{accessToken=[masked], refreshToken=[masked]}";
    }
}
