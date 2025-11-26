package com.ryuqq.gateway.domain.authorization.vo;

import java.util.Objects;
import java.util.Set;

/**
 * EndpointPermission - 엔드포인트 권한 매핑 Value Object
 *
 * <p>API 엔드포인트와 필요한 권한/역할을 매핑하는 불변 객체입니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public record EndpointPermission(
        String serviceName,
        String path,
        HttpMethod method,
        Set<Permission> requiredPermissions,
        Set<String> requiredRoles,
        boolean isPublic) {

    public EndpointPermission {
        Objects.requireNonNull(serviceName, "Service name cannot be null");
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(method, "Method cannot be null");
        requiredPermissions =
                requiredPermissions == null ? Set.of() : Set.copyOf(requiredPermissions);
        requiredRoles = requiredRoles == null ? Set.of() : Set.copyOf(requiredRoles);
    }

    /**
     * 정적 팩토리 메서드
     *
     * @param serviceName 서비스 이름
     * @param path API 경로
     * @param method HTTP 메서드
     * @param requiredPermissions 필수 권한
     * @param requiredRoles 필수 역할
     * @param isPublic 공개 여부
     * @return EndpointPermission 객체
     */
    public static EndpointPermission of(
            String serviceName,
            String path,
            HttpMethod method,
            Set<Permission> requiredPermissions,
            Set<String> requiredRoles,
            boolean isPublic) {
        return new EndpointPermission(
                serviceName, path, method, requiredPermissions, requiredRoles, isPublic);
    }

    /**
     * Public 엔드포인트 생성 (인증 불필요)
     *
     * @param serviceName 서비스 이름
     * @param path API 경로
     * @param method HTTP 메서드
     * @return Public EndpointPermission
     */
    public static EndpointPermission publicEndpoint(
            String serviceName, String path, HttpMethod method) {
        return new EndpointPermission(serviceName, path, method, Set.of(), Set.of(), true);
    }

    /**
     * 권한 검증이 필요한지 확인
     *
     * @return Public이 아니고 권한 또는 역할이 필요한 경우 true
     */
    public boolean requiresAuthorization() {
        return !isPublic && (!requiredPermissions.isEmpty() || !requiredRoles.isEmpty());
    }

    /**
     * 요청 경로와 매칭되는지 확인 (Path Variable 지원)
     *
     * <p>예시:
     *
     * <ul>
     *   <li>/api/v1/orders/{orderId} matches /api/v1/orders/123 → true
     *   <li>/api/v1/orders matches /api/v1/orders → true
     *   <li>/api/v1/orders matches /api/v1/products → false
     * </ul>
     *
     * @param requestPath 요청 경로
     * @return 매칭 여부
     */
    public boolean matchesPath(String requestPath) {
        if (requestPath == null) {
            return false;
        }
        String pathPattern = this.path.replaceAll("\\{[^/]+\\}", "[^/]+");
        return requestPath.matches(pathPattern);
    }
}
