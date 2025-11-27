package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Session Config Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Session Config Entity
 *
 * <p>JSON 직렬화/역직렬화 지원
 *
 * @author development-team
 * @since 1.0.0
 */
public final class SessionConfigEntity {

    private final int maxActiveSessions;
    private final long accessTokenTTLSeconds;
    private final long refreshTokenTTLSeconds;

    /** Constructor (Jackson 역직렬화용) */
    @JsonCreator
    public SessionConfigEntity(
            @JsonProperty("maxActiveSessions") int maxActiveSessions,
            @JsonProperty("accessTokenTTLSeconds") long accessTokenTTLSeconds,
            @JsonProperty("refreshTokenTTLSeconds") long refreshTokenTTLSeconds) {
        this.maxActiveSessions = maxActiveSessions;
        this.accessTokenTTLSeconds = accessTokenTTLSeconds;
        this.refreshTokenTTLSeconds = refreshTokenTTLSeconds;
    }

    // Getters (Jackson 직렬화용)
    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public long getAccessTokenTTLSeconds() {
        return accessTokenTTLSeconds;
    }

    public long getRefreshTokenTTLSeconds() {
        return refreshTokenTTLSeconds;
    }

    @Override
    public String toString() {
        return "SessionConfigEntity{"
                + "maxActiveSessions=" + maxActiveSessions
                + ", accessTokenTTLSeconds=" + accessTokenTTLSeconds
                + ", refreshTokenTTLSeconds=" + refreshTokenTTLSeconds
                + '}';
    }
}
