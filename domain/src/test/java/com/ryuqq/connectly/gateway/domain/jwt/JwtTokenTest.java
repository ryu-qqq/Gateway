package com.ryuqq.connectly.gateway.domain.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtToken Aggregate 테스트")
class JwtTokenTest {

    @Test
    @DisplayName("유효한 데이터로 JwtToken 생성")
    void shouldCreateJwtTokenWithValidData() {
        // Given
        AccessToken accessToken = new AccessToken("eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOjEyM30.signature");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant createdAt = Instant.now();

        // When
        JwtToken jwtToken = new JwtToken(accessToken, expiresAt, createdAt);

        // Then
        assertThat(jwtToken.getAccessToken()).isEqualTo(accessToken);
        assertThat(jwtToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(jwtToken.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("토큰이 만료되지 않았음을 검증")
    void shouldValidateTokenNotExpired() {
        // Given
        AccessToken accessToken = new AccessToken("eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOjEyM30.signature");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant createdAt = Instant.now();

        JwtToken jwtToken = new JwtToken(accessToken, expiresAt, createdAt);

        // When
        boolean expired = jwtToken.isExpired();

        // Then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("토큰이 만료되었음을 검증")
    void shouldValidateTokenExpired() {
        // Given
        AccessToken accessToken = new AccessToken("eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOjEyM30.signature");
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);  // 1시간 전 만료
        Instant createdAt = Instant.now().minus(2, ChronoUnit.HOURS);

        JwtToken jwtToken = new JwtToken(accessToken, expiresAt, createdAt);

        // When
        boolean expired = jwtToken.isExpired();

        // Then
        assertThat(expired).isTrue();
    }
}
