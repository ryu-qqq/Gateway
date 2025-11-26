package com.ryuqq.gateway.adapter.out.redis.mapper;

import com.ryuqq.gateway.adapter.out.redis.entity.EndpointPermissionEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Permission Spec Mapper
 *
 * <p>PermissionSpec Domain ↔ Redis Entity 변환
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionSpecMapper {

    /**
     * Entity → Domain 변환
     *
     * @param entity PermissionSpecEntity
     * @return PermissionSpec
     */
    public PermissionSpec toPermissionSpec(PermissionSpecEntity entity) {
        List<EndpointPermission> permissions =
                entity.getPermissions().stream().map(this::toEndpointPermission).toList();

        return PermissionSpec.of(entity.getVersion(), entity.getUpdatedAt(), permissions);
    }

    /**
     * Domain → Entity 변환
     *
     * @param domain PermissionSpec
     * @return PermissionSpecEntity
     */
    public PermissionSpecEntity toEntity(PermissionSpec domain) {
        List<EndpointPermissionEntity> permissions =
                domain.permissions().stream().map(this::toEndpointPermissionEntity).toList();

        return new PermissionSpecEntity(domain.version(), domain.updatedAt(), permissions);
    }

    private EndpointPermission toEndpointPermission(EndpointPermissionEntity entity) {
        Set<Permission> requiredPermissions =
                entity.getRequiredPermissions().stream()
                        .map(Permission::of)
                        .collect(Collectors.toSet());

        return EndpointPermission.of(
                entity.getServiceName(),
                entity.getPath(),
                HttpMethod.from(entity.getMethod()),
                requiredPermissions,
                entity.getRequiredRoles(),
                entity.isPublic());
    }

    private EndpointPermissionEntity toEndpointPermissionEntity(EndpointPermission domain) {
        Set<String> requiredPermissions =
                domain.requiredPermissions().stream()
                        .map(Permission::value)
                        .collect(Collectors.toSet());

        return new EndpointPermissionEntity(
                domain.serviceName(),
                domain.path(),
                domain.method().name(),
                requiredPermissions,
                domain.requiredRoles(),
                domain.isPublic());
    }
}
