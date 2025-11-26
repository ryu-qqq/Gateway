package com.ryuqq.gateway.application.authorization.dto.response;

import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;

/**
 * ValidatePermissionResponse - 권한 검증 응답
 *
 * @param authorized 권한 검증 성공 여부
 * @param endpointPermission 엔드포인트 권한 정보 (선택)
 * @author development-team
 * @since 1.0.0
 */
public record ValidatePermissionResponse(
        boolean authorized, EndpointPermission endpointPermission) {

    public static ValidatePermissionResponse authorized(EndpointPermission endpointPermission) {
        return new ValidatePermissionResponse(true, endpointPermission);
    }

    public static ValidatePermissionResponse denied() {
        return new ValidatePermissionResponse(false, null);
    }

    public static ValidatePermissionResponse publicEndpoint(EndpointPermission endpointPermission) {
        return new ValidatePermissionResponse(true, endpointPermission);
    }
}
