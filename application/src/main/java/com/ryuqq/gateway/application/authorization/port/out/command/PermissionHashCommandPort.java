package com.ryuqq.gateway.application.authorization.port.out.command;

import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import reactor.core.publisher.Mono;

/**
 * PermissionHashCommandPort - Permission Hash 저장/삭제 Port
 *
 * <p>Redis Cache에 사용자별 Permission Hash를 저장/삭제하는 Port입니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PermissionHashCommandPort {

    /**
     * Permission Hash 저장 (Cache)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param permissionHash Permission Hash
     * @return 완료 Mono
     */
    Mono<Void> save(String tenantId, String userId, PermissionHash permissionHash);

    /**
     * Permission Hash 캐시 무효화
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 완료 Mono
     */
    Mono<Void> invalidate(String tenantId, String userId);
}
