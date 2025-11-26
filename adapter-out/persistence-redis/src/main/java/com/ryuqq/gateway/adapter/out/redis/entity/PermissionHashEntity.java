package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;

/**
 * Permission Hash Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Permission Hash Entity
 *
 * <p><strong>Redis Key</strong>: {@code authhub:permission:hash:{tenantId}:{userId}}
 *
 * <p><strong>TTL</strong>: 30초
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionHashEntity {

    private final String hash;
    private final Set<String> permissions;
    private final Set<String> roles;
    private final Instant generatedAt;

    @JsonCreator
    public PermissionHashEntity(
            @JsonProperty("hash") String hash,
            @JsonProperty("permissions") Set<String> permissions,
            @JsonProperty("roles") Set<String> roles,
            @JsonProperty("generatedAt") Instant generatedAt) {
        this.hash = hash;
        this.permissions = permissions;
        this.roles = roles;
        this.generatedAt = generatedAt;
    }

    public String getHash() {
        return hash;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public String toString() {
        return "PermissionHashEntity{"
                + "hash='"
                + hash
                + '\''
                + ", permissions="
                + (permissions != null ? permissions.size() : 0)
                + ", roles="
                + (roles != null ? roles.size() : 0)
                + ", generatedAt="
                + generatedAt
                + '}';
    }
}
