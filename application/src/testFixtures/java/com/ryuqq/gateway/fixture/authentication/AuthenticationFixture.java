package com.ryuqq.gateway.fixture.authentication;

import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.query.GetPublicKeyQuery;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;

/**
 * Authentication 테스트 Fixture
 *
 * <p>Object Mother Pattern을 사용한 테스트 객체 생성
 *
 * @author development-team
 * @since 1.0.0
 */
public final class AuthenticationFixture {

    private AuthenticationFixture() {}

    // ========================================
    // Default Values
    // ========================================
    public static final String DEFAULT_TENANT_ID = "tenant-001";
    public static final Long DEFAULT_USER_ID = 12345L;
    // JWT Header: {"alg":"RS256","typ":"JWT","kid":"key-001"} base64url encoded
    public static final String DEFAULT_ACCESS_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImtleS0wMDEifQ"
                    + ".eyJzdWIiOiJ0ZXN0In0"
                    + ".signature";
    // Refresh token must be at least 32 characters
    public static final String DEFAULT_REFRESH_TOKEN = "refresh-token-12345-abcdefghijklmnop";
    // New tokens for TokenPair (also need to be valid JWT format)
    // JWT Header: {"alg":"RS256","typ":"JWT","kid":"new-key"} base64url encoded
    public static final String NEW_ACCESS_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Im5ldy1rZXkifQ"
                    + ".eyJzdWIiOiJuZXcifQ"
                    + ".new-signature";
    public static final String NEW_REFRESH_TOKEN = "new-refresh-token-abcdefghijklmnopqr";
    public static final String DEFAULT_KID = "key-001";
    public static final String DEFAULT_MODULUS = "test-modulus-base64";
    public static final String DEFAULT_EXPONENT = "AQAB";

    // ========================================
    // RefreshAccessTokenCommand
    // ========================================
    public static RefreshAccessTokenCommand aRefreshAccessTokenCommand() {
        return RefreshAccessTokenCommand.of(
                DEFAULT_TENANT_ID, DEFAULT_USER_ID, DEFAULT_REFRESH_TOKEN);
    }

    public static RefreshAccessTokenCommand aRefreshAccessTokenCommand(String tenantId) {
        return RefreshAccessTokenCommand.of(tenantId, DEFAULT_USER_ID, DEFAULT_REFRESH_TOKEN);
    }

    public static RefreshAccessTokenCommand aRefreshAccessTokenCommand(
            String tenantId, Long userId, String refreshToken) {
        return RefreshAccessTokenCommand.of(tenantId, userId, refreshToken);
    }

    // ========================================
    // ValidateJwtCommand
    // ========================================
    public static ValidateJwtCommand aValidateJwtCommand() {
        return ValidateJwtCommand.of(DEFAULT_ACCESS_TOKEN);
    }

    public static ValidateJwtCommand aValidateJwtCommand(String accessToken) {
        return ValidateJwtCommand.of(accessToken);
    }

    // ========================================
    // GetPublicKeyQuery
    // ========================================
    public static GetPublicKeyQuery aGetPublicKeyQuery() {
        return GetPublicKeyQuery.of(DEFAULT_KID);
    }

    public static GetPublicKeyQuery aGetPublicKeyQuery(String kid) {
        return GetPublicKeyQuery.of(kid);
    }

    // ========================================
    // Domain VOs
    // ========================================
    public static RefreshToken aRefreshToken() {
        return RefreshToken.of(DEFAULT_REFRESH_TOKEN);
    }

    public static RefreshToken aRefreshToken(String value) {
        return RefreshToken.of(value);
    }

    public static PublicKey aPublicKey() {
        return PublicKey.of(DEFAULT_KID, DEFAULT_MODULUS, DEFAULT_EXPONENT, "RSA", "sig", "RS256");
    }

    public static PublicKey aPublicKey(String kid) {
        return PublicKey.of(kid, DEFAULT_MODULUS, DEFAULT_EXPONENT, "RSA", "sig", "RS256");
    }

    public static TokenPair aTokenPair() {
        return TokenPair.of(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN);
    }

    public static TokenPair aTokenPair(String accessToken, String refreshToken) {
        return TokenPair.of(accessToken, refreshToken);
    }
}
