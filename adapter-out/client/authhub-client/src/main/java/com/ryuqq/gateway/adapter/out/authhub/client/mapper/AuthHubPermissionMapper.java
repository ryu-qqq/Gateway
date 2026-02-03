package com.ryuqq.gateway.adapter.out.authhub.client.mapper;

import com.ryuqq.authhub.sdk.model.internal.EndpointPermissionSpec;
import com.ryuqq.authhub.sdk.model.internal.EndpointPermissionSpecList;
import com.ryuqq.authhub.sdk.model.internal.UserPermissions;
import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.PermissionException;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * AuthHub Permission Mapper
 *
 * <p>Permission 관련 SDK 응답을 Domain 객체로 변환
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubPermissionMapper {

    /**
     * SDK EndpointPermissionSpecList → Domain PermissionSpec 변환
     *
     * @param specList SDK 응답
     * @return PermissionSpec
     */
    public PermissionSpec toPermissionSpec(EndpointPermissionSpecList specList) {
        List<EndpointPermission> endpoints =
                specList.endpoints() != null
                        ? specList.endpoints().stream().map(this::toEndpointPermission).toList()
                        : List.of();

        Long versionHash =
                specList.version() != null
                        ? (long) specList.version().hashCode()
                        : System.currentTimeMillis();

        Instant updatedAt = specList.updatedAt() != null ? specList.updatedAt() : Instant.now();

        return PermissionSpec.of(versionHash, updatedAt, endpoints);
    }

    /**
     * SDK EndpointPermissionSpec → Domain EndpointPermission 변환
     *
     * @param spec SDK Endpoint Permission Spec
     * @return EndpointPermission
     */
    public EndpointPermission toEndpointPermission(EndpointPermissionSpec spec) {
        Set<Permission> permissions =
                spec.requiredPermissions() != null
                        ? spec.requiredPermissions().stream()
                                .map(Permission::of)
                                .collect(Collectors.toSet())
                        : Set.of();

        Set<String> roles =
                spec.requiredRoles() != null ? Set.copyOf(spec.requiredRoles()) : Set.of();

        HttpMethod method = HttpMethod.valueOf(spec.httpMethod().toUpperCase());

        return EndpointPermission.of(
                spec.serviceName(),
                spec.pathPattern(),
                method,
                permissions,
                roles,
                spec.isPublic());
    }

    /**
     * SDK UserPermissions → Domain PermissionHash 변환
     *
     * @param userPermissions SDK 응답
     * @return PermissionHash
     */
    public PermissionHash toPermissionHash(UserPermissions userPermissions) {
        if (userPermissions == null) {
            throw new PermissionException("Empty UserPermissions response");
        }

        Set<String> permissions =
                userPermissions.permissions() != null
                        ? Set.copyOf(userPermissions.permissions())
                        : Set.of();

        Set<String> roles =
                userPermissions.roles() != null ? Set.copyOf(userPermissions.roles()) : Set.of();

        Instant generatedAt =
                userPermissions.generatedAt() != null
                        ? userPermissions.generatedAt()
                        : Instant.now();

        return PermissionHash.fromStrings(userPermissions.hash(), permissions, roles, generatedAt);
    }
}
