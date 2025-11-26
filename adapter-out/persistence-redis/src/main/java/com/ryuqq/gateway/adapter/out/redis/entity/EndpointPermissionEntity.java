package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

/**
 * Endpoint Permission Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Endpoint Permission Entity
 *
 * @author development-team
 * @since 1.0.0
 */
public final class EndpointPermissionEntity {

    private final String serviceName;
    private final String path;
    private final String method;
    private final Set<String> requiredPermissions;
    private final Set<String> requiredRoles;
    private final boolean isPublic;

    @JsonCreator
    public EndpointPermissionEntity(
            @JsonProperty("serviceName") String serviceName,
            @JsonProperty("path") String path,
            @JsonProperty("method") String method,
            @JsonProperty("requiredPermissions") Set<String> requiredPermissions,
            @JsonProperty("requiredRoles") Set<String> requiredRoles,
            @JsonProperty("isPublic") boolean isPublic) {
        this.serviceName = serviceName;
        this.path = path;
        this.method = method;
        this.requiredPermissions = requiredPermissions;
        this.requiredRoles = requiredRoles;
        this.isPublic = isPublic;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public Set<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public Set<String> getRequiredRoles() {
        return requiredRoles;
    }

    @JsonProperty("isPublic")
    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public String toString() {
        return "EndpointPermissionEntity{"
                + "serviceName='"
                + serviceName
                + '\''
                + ", path='"
                + path
                + '\''
                + ", method='"
                + method
                + '\''
                + ", isPublic="
                + isPublic
                + '}';
    }
}
