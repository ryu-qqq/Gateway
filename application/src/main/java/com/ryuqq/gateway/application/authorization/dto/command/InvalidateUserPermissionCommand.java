package com.ryuqq.gateway.application.authorization.dto.command;

/**
 * InvalidateUserPermissionCommand - 사용자 권한 캐시 무효화 Command
 *
 * @param tenantId 테넌트 ID
 * @param userId 사용자 ID
 * @author development-team
 * @since 1.0.0
 */
public record InvalidateUserPermissionCommand(String tenantId, String userId) {

    public static InvalidateUserPermissionCommand of(String tenantId, String userId) {
        return new InvalidateUserPermissionCommand(tenantId, userId);
    }
}
