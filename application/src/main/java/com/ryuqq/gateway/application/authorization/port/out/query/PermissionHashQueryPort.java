package com.ryuqq.gateway.application.authorization.port.out.query;

import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import reactor.core.publisher.Mono;

/**
 * PermissionHashQueryPort - Permission Hash 조회 Port
 *
 * <p>Redis Cache에서 사용자별 Permission Hash를 조회하는 Port입니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PermissionHashQueryPort {

    /**
     * 사용자별 Permission Hash 조회 (Cache)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Permission Hash (Cache Miss 시 empty)
     */
    Mono<PermissionHash> findByTenantAndUser(String tenantId, String userId);
}
