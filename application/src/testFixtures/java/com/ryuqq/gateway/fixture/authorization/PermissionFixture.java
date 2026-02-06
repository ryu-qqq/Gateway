package com.ryuqq.gateway.fixture.authorization;

import com.ryuqq.gateway.domain.authorization.vo.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Permission 테스트용 Fixture (Object Mother Pattern)
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionFixture {

    private static final String DEFAULT_HASH = "test-hash-abc";
    private static final String DEFAULT_SERVICE = "test-service";

    private PermissionFixture() {}

    // ============== PermissionSpec ==============

    /** 기본 PermissionSpec 생성 */
    public static PermissionSpec aDefaultPermissionSpec() {
        return PermissionSpec.of(
                1L,
                Instant.now(),
                List.of(
                        aPublicEndpoint("/api/v1/public", HttpMethod.GET),
                        aProtectedEndpoint("/api/v1/orders", HttpMethod.GET, Set.of("order:read")),
                        aProtectedEndpoint(
                                "/api/v1/admin", HttpMethod.GET, Set.of(), Set.of("ADMIN"))));
    }

    /** 지정된 버전으로 PermissionSpec 생성 */
    public static PermissionSpec aPermissionSpec(Long version) {
        return PermissionSpec.of(
                version, Instant.now(), List.of(aPublicEndpoint("/api/v1/test", HttpMethod.GET)));
    }

    /** 지정된 엔드포인트로 PermissionSpec 생성 */
    public static PermissionSpec aPermissionSpec(List<EndpointPermission> endpoints) {
        return PermissionSpec.of(1L, Instant.now(), endpoints);
    }

    /** 빈 PermissionSpec 생성 */
    public static PermissionSpec anEmptyPermissionSpec() {
        return PermissionSpec.of(1L, Instant.now(), List.of());
    }

    // ============== PermissionHash ==============

    /** 기본 PermissionHash 생성 */
    public static PermissionHash aDefaultPermissionHash() {
        return PermissionHash.of(
                DEFAULT_HASH,
                Set.of(Permission.of("order:read"), Permission.of("order:create")),
                Set.of("USER"),
                Instant.now());
    }

    /** 지정된 권한으로 PermissionHash 생성 */
    public static PermissionHash aPermissionHash(String hash, Set<Permission> permissions) {
        return PermissionHash.of(hash, permissions, Set.of("USER"), Instant.now());
    }

    /** 지정된 권한과 역할로 PermissionHash 생성 */
    public static PermissionHash aPermissionHash(
            String hash, Set<Permission> permissions, Set<String> roles) {
        return PermissionHash.of(hash, permissions, roles, Instant.now());
    }

    /** 와일드카드 권한을 가진 PermissionHash 생성 */
    public static PermissionHash aPermissionHashWithWildcard(
            String hash, String wildcardPermission) {
        return PermissionHash.of(
                hash, Set.of(Permission.of(wildcardPermission)), Set.of("USER"), Instant.now());
    }

    /** Admin 역할의 PermissionHash 생성 */
    public static PermissionHash anAdminPermissionHash(String hash) {
        return PermissionHash.of(hash, Set.of(), Set.of("ADMIN"), Instant.now());
    }

    /** 빈 권한의 PermissionHash 생성 */
    public static PermissionHash anEmptyPermissionHash(String hash) {
        return PermissionHash.of(hash, Set.of(), Set.of(), Instant.now());
    }

    // ============== EndpointPermission ==============

    /** Public 엔드포인트 생성 */
    public static EndpointPermission aPublicEndpoint(String path, HttpMethod method) {
        return EndpointPermission.publicEndpoint(DEFAULT_SERVICE, path, method);
    }

    /** 권한이 필요한 Protected 엔드포인트 생성 */
    public static EndpointPermission aProtectedEndpoint(
            String path, HttpMethod method, Set<String> permissionStrings) {
        Set<Permission> permissions =
                permissionStrings.stream()
                        .map(Permission::of)
                        .collect(java.util.stream.Collectors.toSet());
        return EndpointPermission.of(DEFAULT_SERVICE, path, method, permissions, Set.of(), false);
    }

    /** 권한과 역할이 필요한 Protected 엔드포인트 생성 */
    public static EndpointPermission aProtectedEndpoint(
            String path, HttpMethod method, Set<String> permissionStrings, Set<String> roles) {
        Set<Permission> permissions =
                permissionStrings.stream()
                        .map(Permission::of)
                        .collect(java.util.stream.Collectors.toSet());
        return EndpointPermission.of(DEFAULT_SERVICE, path, method, permissions, roles, false);
    }

    /** 권한이 필요 없는 비공개 엔드포인트 생성 */
    public static EndpointPermission anInternalEndpoint(String path, HttpMethod method) {
        return EndpointPermission.of(DEFAULT_SERVICE, path, method, Set.of(), Set.of(), false);
    }

    // ============== Permission ==============

    /** Permission 생성 */
    public static Permission aPermission(String value) {
        return Permission.of(value);
    }

    /** Permission Set 생성 */
    public static Set<Permission> permissions(String... values) {
        return java.util.Arrays.stream(values)
                .map(Permission::of)
                .collect(java.util.stream.Collectors.toSet());
    }
}
