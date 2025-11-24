package com.ryuqq.connectly.gateway.domain.jwt;

import java.time.Instant;
import java.util.Objects;

/**
 * JWT Token Aggregate Root
 * JWT 토큰의 생명주기와 만료 검증을 담당하는 도메인 객체
 *
 * Zero-Tolerance 규칙:
 * - Lombok 금지 (Plain Java 사용)
 * - 불변성 보장 (final 필드)
 * - Law of Demeter 준수
 */
public final class JwtToken {

    private final AccessToken accessToken;
    private final Instant expiresAt;
    private final Instant createdAt;

    public JwtToken(AccessToken accessToken, Instant expiresAt, Instant createdAt) {
        if (accessToken == null) {
            throw new IllegalArgumentException("AccessToken cannot be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }

        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /**
     * 토큰 만료 여부 검증
     *
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // Getters (Lombok 사용 안함)
    public AccessToken getAccessToken() {
        return accessToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtToken jwtToken = (JwtToken) o;
        return Objects.equals(accessToken, jwtToken.accessToken) &&
               Objects.equals(expiresAt, jwtToken.expiresAt) &&
               Objects.equals(createdAt, jwtToken.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, expiresAt, createdAt);
    }

    @Override
    public String toString() {
        return "JwtToken{" +
               "accessToken=" + accessToken +
               ", expiresAt=" + expiresAt +
               ", createdAt=" + createdAt +
               '}';
    }
}
