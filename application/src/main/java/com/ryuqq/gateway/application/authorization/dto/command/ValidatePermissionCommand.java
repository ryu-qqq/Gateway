package com.ryuqq.gateway.application.authorization.dto.command;

import java.util.Objects;
import java.util.Set;

/**
 * ValidatePermissionCommand - 권한 검증 요청 Command
 *
 * @param userId 사용자 ID
 * @param tenantId 테넌트 ID
 * @param permissionHash JWT의 권한 해시
 * @param roles 사용자 역할 목록
 * @param requestPath 요청 경로
 * @param requestMethod HTTP 메서드
 * @author development-team
 * @since 1.0.0
 */
public record ValidatePermissionCommand(
        String userId,
        String tenantId,
        String permissionHash,
        Set<String> roles,
        String requestPath,
        String requestMethod) {

    public ValidatePermissionCommand {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(requestPath, "requestPath cannot be null");
        Objects.requireNonNull(requestMethod, "requestMethod cannot be null");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static ValidatePermissionCommand of(
            String userId,
            String tenantId,
            String permissionHash,
            Set<String> roles,
            String requestPath,
            String requestMethod) {
        return new ValidatePermissionCommand(
                userId, tenantId, permissionHash, roles, requestPath, requestMethod);
    }
}
