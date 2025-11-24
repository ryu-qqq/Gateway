package com.ryuqq.connectly.gateway.domain.jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * JwtToken 테스트용 Fixture (Object Mother Pattern)
 * 테스트 데이터 생성을 중앙화하고 재사용성을 높임
 */
public class JwtTokenFixture {

    private static final String VALID_JWT = "eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOjEyM30.signature";

    /**
     * 유효한 JwtToken 생성 (1시간 후 만료)
     */
    public static JwtToken aValidJwtToken() {
        AccessToken accessToken = new AccessToken(VALID_JWT);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        return new JwtToken(accessToken, expiresAt, now);
    }

    /**
     * 만료된 JwtToken 생성 (1시간 전 만료)
     */
    public static JwtToken anExpiredJwtToken() {
        AccessToken accessToken = new AccessToken(VALID_JWT);
        Instant now = Instant.now();
        Instant expiresAt = now.minus(1, ChronoUnit.HOURS);
        Instant createdAt = now.minus(2, ChronoUnit.HOURS);

        return new JwtToken(accessToken, expiresAt, createdAt);
    }

    /**
     * 커스텀 만료 시간으로 JwtToken 생성
     *
     * @param expiresAt 만료 시간
     */
    public static JwtToken aJwtTokenWithExpiry(Instant expiresAt) {
        AccessToken accessToken = new AccessToken(VALID_JWT);
        Instant createdAt = Instant.now();

        return new JwtToken(accessToken, expiresAt, createdAt);
    }

    /**
     * 커스텀 AccessToken으로 JwtToken 생성
     *
     * @param accessToken JWT Access Token
     */
    public static JwtToken aJwtTokenWithAccessToken(AccessToken accessToken) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        return new JwtToken(accessToken, expiresAt, now);
    }
}
