package com.ryuqq.gateway.domain.authorization.vo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * PermissionSpec - Permission Spec Value Object
 *
 * <p>전체 Permission Spec을 나타내는 불변 객체입니다. 모든 엔드포인트의 권한 정보를 포함합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public record PermissionSpec(
        Long version, Instant updatedAt, List<EndpointPermission> permissions) {

    public PermissionSpec {
        Objects.requireNonNull(version, "Version cannot be null");
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    /**
     * 정적 팩토리 메서드
     *
     * @param version Spec 버전
     * @param updatedAt 마지막 업데이트 시각
     * @param permissions 엔드포인트 권한 목록
     * @return PermissionSpec 객체
     */
    public static PermissionSpec of(
            Long version, Instant updatedAt, List<EndpointPermission> permissions) {
        return new PermissionSpec(version, updatedAt, permissions);
    }

    /**
     * 요청 경로와 메서드에 해당하는 엔드포인트 권한 찾기
     *
     * @param requestPath 요청 경로
     * @param method HTTP 메서드
     * @return 매칭되는 EndpointPermission (없으면 Optional.empty)
     */
    public Optional<EndpointPermission> findPermission(String requestPath, HttpMethod method) {
        return permissions.stream()
                .filter(ep -> ep.method() == method && ep.matchesPath(requestPath))
                .findFirst();
    }

    /**
     * 특정 서비스의 엔드포인트 권한 목록 반환
     *
     * @param serviceName 서비스 이름
     * @return 해당 서비스의 엔드포인트 권한 목록
     */
    public List<EndpointPermission> findByServiceName(String serviceName) {
        return permissions.stream().filter(ep -> ep.serviceName().equals(serviceName)).toList();
    }

    /**
     * 버전 비교
     *
     * @param otherVersion 비교할 버전
     * @return 현재 버전이 더 최신이면 true
     */
    public boolean isNewerThan(Long otherVersion) {
        return version > otherVersion;
    }

    /**
     * Public 엔드포인트 목록 반환
     *
     * @return Public 엔드포인트 목록
     */
    public List<EndpointPermission> publicEndpoints() {
        return permissions.stream().filter(EndpointPermission::isPublic).toList();
    }

    /**
     * 권한이 필요한 엔드포인트 목록 반환
     *
     * @return 권한이 필요한 엔드포인트 목록
     */
    public List<EndpointPermission> protectedEndpoints() {
        return permissions.stream().filter(EndpointPermission::requiresAuthorization).toList();
    }
}
