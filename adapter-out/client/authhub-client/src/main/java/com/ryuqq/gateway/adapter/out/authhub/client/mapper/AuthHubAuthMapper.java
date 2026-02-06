package com.ryuqq.gateway.adapter.out.authhub.client.mapper;

import com.ryuqq.authhub.sdk.model.internal.PublicKeys;
import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.AuthException;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * AuthHub Auth Mapper
 *
 * <p>인증 관련 SDK 응답을 Domain 객체로 변환
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubAuthMapper {

    /**
     * SDK PublicKeys → PublicKey 리스트 변환
     *
     * @param publicKeys SDK 응답
     * @return PublicKey 리스트
     */
    public List<PublicKey> toPublicKeys(PublicKeys publicKeys) {
        if (publicKeys == null || publicKeys.keys() == null || publicKeys.keys().isEmpty()) {
            throw new AuthException("Empty JWKS response from AuthHub");
        }
        return publicKeys.keys().stream().map(this::toPublicKey).toList();
    }

    /**
     * SDK PublicKey → Domain PublicKey 변환
     *
     * <p>SDK PublicKey 필드 순서: kid, kty, use, alg, n, e
     *
     * @param sdkPublicKey SDK Public Key
     * @return Domain PublicKey
     */
    public PublicKey toPublicKey(com.ryuqq.authhub.sdk.model.internal.PublicKey sdkPublicKey) {
        // SDK 필드 순서: kid, kty, use, alg, n, e
        // Domain 필드 순서: kid, n, e, kty, use, alg
        return new PublicKey(
                sdkPublicKey.kid(),
                sdkPublicKey.n(),
                sdkPublicKey.e(),
                sdkPublicKey.kty(),
                sdkPublicKey.use(),
                sdkPublicKey.alg());
    }

    /**
     * Refresh Token Response → TokenPair 변환
     *
     * @param accessToken Access Token
     * @param refreshToken Refresh Token
     * @return TokenPair
     */
    public TokenPair toTokenPair(String accessToken, String refreshToken) {
        if (accessToken == null || refreshToken == null) {
            throw new AuthException("Invalid refresh token response from AuthHub");
        }
        return TokenPair.of(accessToken, refreshToken);
    }
}
