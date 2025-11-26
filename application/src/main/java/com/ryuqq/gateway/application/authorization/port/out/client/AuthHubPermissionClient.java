package com.ryuqq.gateway.application.authorization.port.out.client;

import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import reactor.core.publisher.Mono;

/**
 * AuthHubPermissionClient - AuthHub Permission API Client Port
 *
 * <p>AuthHub에서 Permission 정보를 조회하는 Port입니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface AuthHubPermissionClient {

    /**
     * Permission Spec 조회 (AuthHub API)
     *
     * @return Permission Spec
     */
    Mono<PermissionSpec> fetchPermissionSpec();

    /**
     * 사용자별 Permission Hash 조회 (AuthHub API)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Permission Hash
     */
    Mono<PermissionHash> fetchUserPermissions(String tenantId, String userId);
}
