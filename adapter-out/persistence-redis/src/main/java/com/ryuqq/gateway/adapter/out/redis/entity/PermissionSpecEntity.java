package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Permission Spec Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Permission Spec Entity
 *
 * <p><strong>Redis Key</strong>: {@code authhub:permission:spec}
 *
 * <p><strong>TTL</strong>: 30초
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionSpecEntity {

    private final Long version;
    private final Instant updatedAt;
    private final List<EndpointPermissionEntity> permissions;

    @JsonCreator
    public PermissionSpecEntity(
            @JsonProperty("version") Long version,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("permissions") List<EndpointPermissionEntity> permissions) {
        this.version = version;
        this.updatedAt = updatedAt;
        this.permissions = permissions;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<EndpointPermissionEntity> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return "PermissionSpecEntity{"
                + "version="
                + version
                + ", updatedAt="
                + updatedAt
                + ", permissions="
                + (permissions != null ? permissions.size() : 0)
                + '}';
    }
}
