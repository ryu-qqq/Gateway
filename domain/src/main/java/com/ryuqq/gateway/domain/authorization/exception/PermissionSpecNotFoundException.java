package com.ryuqq.gateway.domain.authorization.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import java.util.Map;

/**
 * PermissionSpecNotFoundException - Permission Spec 미정의 예외
 *
 * <p>요청한 엔드포인트에 대한 Permission Spec이 정의되지 않았을 때 발생합니다. Default Deny 정책에 따라 403 Forbidden을 반환합니다.
 *
 * <p><strong>HTTP 응답:</strong> 403 Forbidden
 *
 * @author development-team
 * @since 1.0.0
 */
public class PermissionSpecNotFoundException extends DomainException {

    private final String path;
    private final String method;

    public PermissionSpecNotFoundException(String path, String method) {
        super(
                AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getCode(),
                buildMessage(path, method),
                Map.of("path", path, "method", method));
        this.path = path;
        this.method = method;
    }

    public String path() {
        return path;
    }

    public String method() {
        return method;
    }

    private static String buildMessage(String path, String method) {
        return String.format(
                "Permission spec not found for endpoint: %s %s (Default Deny)", method, path);
    }
}
