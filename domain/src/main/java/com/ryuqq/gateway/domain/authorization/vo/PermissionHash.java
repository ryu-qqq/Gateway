package com.ryuqq.gateway.domain.authorization.vo;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PermissionHash - 권한 해시 Value Object
 *
 * <p>사용자의 권한 집합과 해시를 나타내는 불변 객체입니다. JWT의 permissionHash와 비교하여 권한 변경을 감지합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public record PermissionHash(
        String hash, Set<Permission> permissions, Set<String> roles, Instant generatedAt) {

    public PermissionHash {
        Objects.requireNonNull(hash, "Hash cannot be null");
        if (hash.isBlank()) {
            throw new IllegalArgumentException("Hash cannot be blank");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    /**
     * 정적 팩토리 메서드
     *
     * @param hash SHA-256 해시
     * @param permissions 권한 집합
     * @param roles 역할 집합
     * @param generatedAt 생성 시각
     * @return PermissionHash 객체
     */
    public static PermissionHash of(
            String hash, Set<Permission> permissions, Set<String> roles, Instant generatedAt) {
        return new PermissionHash(hash, permissions, roles, generatedAt);
    }

    /**
     * 권한 문자열 집합으로 생성
     *
     * @param hash SHA-256 해시
     * @param permissionStrings 권한 문자열 집합
     * @param roles 역할 집합
     * @param generatedAt 생성 시각
     * @return PermissionHash 객체
     */
    public static PermissionHash fromStrings(
            String hash, Set<String> permissionStrings, Set<String> roles, Instant generatedAt) {
        Set<Permission> permissions =
                permissionStrings.stream().map(Permission::of).collect(Collectors.toSet());
        return new PermissionHash(hash, permissions, roles, generatedAt);
    }

    /**
     * 해시 값 비교
     *
     * @param otherHash 비교할 해시
     * @return 동일 여부
     */
    public boolean matchesHash(String otherHash) {
        return hash.equals(otherHash);
    }

    /**
     * 권한 보유 여부 확인
     *
     * @param permission 확인할 권한
     * @return 보유 여부 (와일드카드 매칭 포함)
     */
    public boolean hasPermission(Permission permission) {
        return permissions.stream().anyMatch(p -> p.includes(permission));
    }

    /**
     * 모든 필수 권한 보유 여부 확인
     *
     * @param requiredPermissions 필수 권한 집합
     * @return 모든 권한 보유 여부
     */
    public boolean hasAllPermissions(Set<Permission> requiredPermissions) {
        return requiredPermissions.stream().allMatch(this::hasPermission);
    }

    /**
     * 역할 보유 여부 확인
     *
     * @param role 확인할 역할
     * @return 보유 여부
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * 필수 역할 중 하나라도 보유 여부 확인
     *
     * @param requiredRoles 필수 역할 집합
     * @return 하나 이상 보유 여부
     */
    public boolean hasAnyRole(Set<String> requiredRoles) {
        return requiredRoles.stream().anyMatch(this::hasRole);
    }

    /**
     * 권한 문자열 집합 반환
     *
     * @return 권한 문자열 집합
     */
    public Set<String> permissionStrings() {
        return permissions.stream().map(Permission::value).collect(Collectors.toSet());
    }
}
