package com.ryuqq.gateway.integration.fixtures;

import java.time.Instant;
import java.util.List;

/**
 * Permission Test Fixture
 *
 * <p>Integration Test를 위한 Permission 관련 테스트 데이터 생성 유틸리티
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionTestFixture {

    public static final String DEFAULT_TENANT_ID = "tenant-001";
    public static final String DEFAULT_USER_ID = "user-123";
    public static final String ADMIN_USER_ID = "admin-456";
    public static final String DEFAULT_PERMISSION_HASH = "abc123hash";

    private PermissionTestFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Permission Spec 응답 JSON 생성
     *
     * @return Permission Spec JSON
     */
    public static String permissionSpecResponse() {
        return """
               {
                   "version": 1,
                   "updatedAt": "%s",
                   "permissions": [
                       {
                           "serviceName": "test-service",
                           "path": "/test/public",
                           "method": "GET",
                           "requiredPermissions": [],
                           "requiredRoles": [],
                           "isPublic": true
                       },
                       {
                           "serviceName": "test-service",
                           "path": "/test/resource",
                           "method": "GET",
                           "requiredPermissions": ["resource:read"],
                           "requiredRoles": [],
                           "isPublic": false
                       },
                       {
                           "serviceName": "test-service",
                           "path": "/test/resource",
                           "method": "POST",
                           "requiredPermissions": ["resource:write"],
                           "requiredRoles": [],
                           "isPublic": false
                       },
                       {
                           "serviceName": "test-service",
                           "path": "/test/admin",
                           "method": "GET",
                           "requiredPermissions": ["admin:*"],
                           "requiredRoles": ["ADMIN"],
                           "isPublic": false
                       },
                       {
                           "serviceName": "test-service",
                           "path": "/test/users/{userId}",
                           "method": "GET",
                           "requiredPermissions": ["user:read"],
                           "requiredRoles": [],
                           "isPublic": false
                       }
                   ]
               }
               """
                .formatted(Instant.now().toString());
    }

    /**
     * 일반 사용자 Permission Hash 응답 JSON 생성
     *
     * @return Permission Hash JSON
     */
    public static String userPermissionHashResponse() {
        return """
               {
                   "hash": "%s",
                   "permissions": ["resource:read", "resource:write", "user:read"],
                   "roles": ["USER"],
                   "generatedAt": "%s"
               }
               """
                .formatted(DEFAULT_PERMISSION_HASH, Instant.now().toString());
    }

    /**
     * 관리자 Permission Hash 응답 JSON 생성
     *
     * @return Permission Hash JSON
     */
    public static String adminPermissionHashResponse() {
        return """
               {
                   "hash": "admin-hash-456",
                   "permissions": ["resource:read", "resource:write", "user:read", "admin:*"],
                   "roles": ["ADMIN", "USER"],
                   "generatedAt": "%s"
               }
               """
                .formatted(Instant.now().toString());
    }

    /**
     * 권한 없는 사용자 Permission Hash 응답 JSON 생성
     *
     * @return Permission Hash JSON
     */
    public static String noPermissionHashResponse() {
        return """
               {
                   "hash": "empty-hash-789",
                   "permissions": [],
                   "roles": [],
                   "generatedAt": "%s"
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
