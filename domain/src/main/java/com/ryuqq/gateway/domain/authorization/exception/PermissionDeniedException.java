package com.ryuqq.gateway.domain.authorization.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import java.util.Map;
import java.util.Set;

/**
 * PermissionDeniedException - 권한 부족 예외
 *
 * <p>사용자가 요청한 리소스에 대한 권한이 없을 때 발생합니다.
 *
 * <p><strong>HTTP 응답:</strong> 403 Forbidden
 *
 * @author development-team
 * @since 1.0.0
 */
public class PermissionDeniedException extends DomainException {

    private final Set<String> requiredPermissions;
    private final Set<String> userPermissions;

    public PermissionDeniedException(Set<String> requiredPermissions, Set<String> userPermissions) {
        super(
                AuthorizationErrorCode.PERMISSION_DENIED.getCode(),
                buildMessage(requiredPermissions, userPermissions),
                Map.of(
                        "requiredPermissions", requiredPermissions,
                        "userPermissions", userPermissions));
        this.requiredPermissions = Set.copyOf(requiredPermissions);
        this.userPermissions = Set.copyOf(userPermissions);
    }

    public PermissionDeniedException(String message) {
        super(AuthorizationErrorCode.PERMISSION_DENIED.getCode(), message);
        this.requiredPermissions = Set.of();
        this.userPermissions = Set.of();
    }

    public Set<String> requiredPermissions() {
        return requiredPermissions;
    }

    public Set<String> userPermissions() {
        return userPermissions;
    }

    private static String buildMessage(Set<String> required, Set<String> user) {
        return String.format("Permission denied. Required: %s, User has: %s", required, user);
    }
}
