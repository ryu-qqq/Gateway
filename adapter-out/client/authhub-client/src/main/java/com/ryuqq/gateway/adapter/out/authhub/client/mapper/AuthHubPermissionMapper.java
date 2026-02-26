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

    private static final long DEFAULT_VERSION_HASH = 0L;

    /**
     * SDK EndpointPermissionSpecList → Domain PermissionSpec 변환
     *
     * @param specList SDK 응답
     * @return PermissionSpec
     * @throws PermissionException specList가 null일 경우
     */
    public PermissionSpec toPermissionSpec(EndpointPermissionSpecList specList) {
        if (specList == null) {
            throw new PermissionException("Empty EndpointPermissionSpecList response");
        }

        List<EndpointPermission> endpoints = mapEndpoints(specList.endpoints());
        Long versionHash = computeVersionHash(specList.version());
        Instant updatedAt = resolveUpdatedAt(specList.updatedAt());

        return PermissionSpec.of(versionHash, updatedAt, endpoints);
    }

    /**
     * Endpoints 리스트 변환
     *
     * @param endpoints SDK endpoints
     * @return Domain EndpointPermission 리스트
     */
    private List<EndpointPermission> mapEndpoints(List<EndpointPermissionSpec> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }
        return endpoints.stream().map(this::toEndpointPermission).toList();
    }

    /**
     * Version Hash 계산
     *
     * <p>version이 null이면 DEFAULT_VERSION_HASH(0)를 반환합니다. 시간 기반 값을 사용하지 않아 테스트 가능하고 예측 가능합니다.
     *
     * @param version SDK version 문자열
     * @return version hash 값
     */
    private Long computeVersionHash(String version) {
        if (version == null || version.isBlank()) {
            return DEFAULT_VERSION_HASH;
        }
        return (long) version.hashCode();
    }

    /**
     * UpdatedAt 시간 결정
     *
     * @param updatedAt SDK updatedAt
     * @return 유효한 Instant
     * @throws PermissionException updatedAt이 null일 경우
     */
    private Instant resolveUpdatedAt(Instant updatedAt) {
        if (updatedAt == null) {
            throw new PermissionException(
                    "updatedAt field is missing in the permission spec response from AuthHub");
        }
        return updatedAt;
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

        Instant generatedAt = resolveGeneratedAt(userPermissions.generatedAt());

        return PermissionHash.fromStrings(userPermissions.hash(), permissions, roles, generatedAt);
    }

    /**
     * GeneratedAt 시간 결정
     *
     * @param generatedAt SDK generatedAt
     * @return 유효한 Instant
     * @throws PermissionException generatedAt이 null일 경우
     */
    private Instant resolveGeneratedAt(Instant generatedAt) {
        if (generatedAt == null) {
            throw new PermissionException(
                    "generatedAt field is missing in the user permissions response from AuthHub");
        }
        return generatedAt;
    }
}
