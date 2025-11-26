package com.ryuqq.gateway.adapter.out.redis.mapper;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Permission Hash Mapper
 *
 * <p>PermissionHash Domain ↔ Redis Entity 변환
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionHashMapper {

    /**
     * Entity → Domain 변환
     *
     * @param entity PermissionHashEntity
     * @return PermissionHash
     */
    public PermissionHash toPermissionHash(PermissionHashEntity entity) {
        Set<Permission> permissions =
                entity.getPermissions().stream().map(Permission::of).collect(Collectors.toSet());

        return PermissionHash.of(
                entity.getHash(), permissions, entity.getRoles(), entity.getGeneratedAt());
    }

    /**
     * Domain → Entity 변환
     *
     * @param domain PermissionHash
     * @return PermissionHashEntity
     */
    public PermissionHashEntity toEntity(PermissionHash domain) {
        Set<String> permissions =
                domain.permissions().stream().map(Permission::value).collect(Collectors.toSet());

        return new PermissionHashEntity(
                domain.hash(), permissions, domain.roles(), domain.generatedAt());
    }
}
