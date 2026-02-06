package com.ryuqq.gateway.integration.helper;

import java.time.Instant;
import java.util.List;

/**
 * Permission Test Fixture
 *
 * <p>Integration Test를 위한 Permission 관련 테스트 데이터 생성 유틸리티
 *
 * <p><strong>SDK 엔드포인트 경로</strong>:
 *
 * <ul>
 *   <li>Permission Spec: /api/v1/internal/endpoint-permissions/spec
 *   <li>User Permissions: /api/v1/internal/users/{userId}/permissions
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionTestFixture {

    public static final String DEFAULT_TENANT_ID = "tenant-001";
    public static final String DEFAULT_USER_ID = "user-123";
    public static final String ADMIN_USER_ID = "admin-456";
    public static final String DEFAULT_PERMISSION_HASH = "abc123hash";

    /** SDK가 호출하는 Permission Spec 엔드포인트 경로 */
    public static final String PERMISSION_SPEC_PATH = "/api/v1/internal/endpoint-permissions/spec";

    /** SDK가 호출하는 User Permissions 엔드포인트 경로 패턴 (urlPathMatching용) */
    public static final String USER_PERMISSIONS_PATH_PATTERN =
            "/api/v1/internal/users/.+/permissions";

    private PermissionTestFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 모든 경로를 public으로 설정하는 Permission Spec 응답 JSON 생성
     *
     * <p>SDK ApiResponse 형식 + EndpointPermissionSpecList 형식
     *
     * @param pathPattern public으로 설정할 경로 패턴 (예: "/test/.*")
     * @return Permission Spec JSON (SDK 형식)
     */
    public static String allPublicPermissionSpec(String pathPattern) {
        return """
               {
                   "success": true,
                   "data": {
                       "version": "v1.0",
                       "updatedAt": "2025-01-01T00:00:00Z",
                       "endpoints": [
                           {
                               "serviceName": "test-service",
                               "pathPattern": "%s",
                               "httpMethod": "GET",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "All public test endpoint (GET)"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "%s",
                               "httpMethod": "POST",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "All public test endpoint (POST)"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "%s",
                               "httpMethod": "PUT",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "All public test endpoint (PUT)"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "%s",
                               "httpMethod": "DELETE",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "All public test endpoint (DELETE)"
                           }
                       ]
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """
                .formatted(pathPattern, pathPattern, pathPattern, pathPattern);
    }

    /**
     * Host-based routing 테스트용 Permission Spec 응답 JSON 생성
     *
     * <p>Legacy Web/Admin 서비스의 모든 경로를 public으로 설정
     *
     * @return Permission Spec JSON (SDK 형식)
     */
    public static String legacyServicesPermissionSpec() {
        return """
               {
                   "success": true,
                   "data": {
                       "version": "v1.0",
                       "updatedAt": "2025-01-01T00:00:00Z",
                       "endpoints": [
                           {
                               "serviceName": "legacy-web",
                               "pathPattern": "/.*",
                               "httpMethod": "GET",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Legacy web all public"
                           },
                           {
                               "serviceName": "legacy-admin",
                               "pathPattern": "/.*",
                               "httpMethod": "GET",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Legacy admin all public"
                           }
                       ]
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """;
    }

    /**
     * Permission Spec 응답 JSON 생성 (권한 검증 테스트용)
     *
     * <p>Public / Protected / Admin 엔드포인트를 포함
     *
     * @return Permission Spec JSON (SDK 형식)
     */
    public static String permissionSpecResponse() {
        return """
               {
                   "success": true,
                   "data": {
                       "version": "v1.0",
                       "updatedAt": "%s",
                       "endpoints": [
                           {
                               "serviceName": "test-service",
                               "pathPattern": "/test/public",
                               "httpMethod": "GET",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Public endpoint"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "/test/resource",
                               "httpMethod": "GET",
                               "requiredPermissions": ["resource:read"],
                               "requiredRoles": [],
                               "isPublic": false,
                               "description": "Protected resource read"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "/test/resource",
                               "httpMethod": "POST",
                               "requiredPermissions": ["resource:write"],
                               "requiredRoles": [],
                               "isPublic": false,
                               "description": "Protected resource write"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "/test/admin",
                               "httpMethod": "GET",
                               "requiredPermissions": ["admin:*"],
                               "requiredRoles": ["ADMIN"],
                               "isPublic": false,
                               "description": "Admin endpoint"
                           },
                           {
                               "serviceName": "test-service",
                               "pathPattern": "/test/users/{userId}",
                               "httpMethod": "GET",
                               "requiredPermissions": ["user:read"],
                               "requiredRoles": [],
                               "isPublic": false,
                               "description": "User detail endpoint"
                           }
                       ]
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """
                .formatted(Instant.now().toString());
    }

    /**
     * 일반 사용자 Permission Hash 응답 JSON 생성 (SDK ApiResponse 형식)
     *
     * @return User Permission Hash JSON (SDK 형식)
     */
    public static String userPermissionHashResponse() {
        return """
               {
                   "success": true,
                   "data": {
                       "userId": "%s",
                       "hash": "%s",
                       "permissions": ["resource:read", "resource:write", "user:read"],
                       "roles": ["USER"],
                       "generatedAt": "%s"
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """
                .formatted(DEFAULT_USER_ID, DEFAULT_PERMISSION_HASH, Instant.now().toString());
    }

    /**
     * 관리자 Permission Hash 응답 JSON 생성 (SDK ApiResponse 형식)
     *
     * @return Admin Permission Hash JSON (SDK 형식)
     */
    public static String adminPermissionHashResponse() {
        return """
               {
                   "success": true,
                   "data": {
                       "userId": "%s",
                       "hash": "admin-hash-456",
                       "permissions": ["resource:read", "resource:write", "user:read", "admin:*"],
                       "roles": ["ADMIN", "USER"],
                       "generatedAt": "%s"
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """
                .formatted(ADMIN_USER_ID, Instant.now().toString());
    }

    /**
     * 권한 없는 사용자 Permission Hash 응답 JSON 생성 (SDK ApiResponse 형식)
     *
     * @return No Permission Hash JSON (SDK 형식)
     */
    public static String noPermissionHashResponse() {
        return """
               {
                   "success": true,
                   "data": {
                       "userId": "no-permission-user",
                       "hash": "empty-hash-789",
                       "permissions": [],
                       "roles": [],
                       "generatedAt": "%s"
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """
                .formatted(Instant.now().toString());
    }

    /**
     * Spec Sync Webhook 요청 JSON 생성
     *
     * @param version 버전
     * @param changedServices 변경된 서비스 목록
     * @return Webhook 요청 JSON
     */
    public static String specSyncRequest(Long version, List<String> changedServices) {
        return """
               {
                   "version": %d,
                   "changedServices": %s
               }
               """
                .formatted(version, toJsonArray(changedServices));
    }

    /**
     * User Invalidate Webhook 요청 JSON 생성
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Webhook 요청 JSON
     */
    public static String userInvalidateRequest(String tenantId, String userId) {
        return """
               {
                   "tenantId": "%s",
                   "userId": "%s"
               }
               """
                .formatted(tenantId, userId);
    }

    private static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
