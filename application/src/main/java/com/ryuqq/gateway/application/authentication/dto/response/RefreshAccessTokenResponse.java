package com.ryuqq.gateway.application.authentication.dto.response;

import com.ryuqq.gateway.domain.authentication.vo.TokenPair;

/**
 * Access Token Refresh Response DTO
 *
 * <p>Access Token Refresh 결과를 담는 불변 Response 객체
 *
 * <p><strong>포함 정보</strong>:
 *
 * <ul>
 *   <li>tokenPair: 새로 발급된 Access Token + Refresh Token
 * </ul>
 *
 * @param tokenPair 새로 발급된 Token Pair (Access Token + Refresh Token)
 */
public record RefreshAccessTokenResponse(TokenPair tokenPair) {

    /**
     * Factory Method - TokenPair로 Response 생성
     *
     * @param tokenPair 발급된 Token Pair
     * @return RefreshAccessTokenResponse
     */
    public static RefreshAccessTokenResponse from(TokenPair tokenPair) {
        return new RefreshAccessTokenResponse(tokenPair);
    }

    /**
     * Access Token 값 조회
     *
     * @return Access Token 문자열
     */
    public String accessTokenValue() {
        return tokenPair.getAccessToken().getValue();
    }

    /**
     * Refresh Token 값 조회
     *
     * @return Refresh Token 문자열
     */
    public String refreshTokenValue() {
        return tokenPair.getRefreshToken().getValue();
    }
}
