package com.ryuqq.gateway.application.authorization.service.query;

import com.ryuqq.gateway.application.authorization.internal.PermissionHashCoordinator;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Permission Hash 조회 Service
 *
 * <p>PermissionHashCoordinator를 통해 Permission Hash를 조회하는 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * GetPermissionHashService (Application Service)
 *   ↓ (calls)
 * PermissionHashCoordinator (Application Manager - internal)
 *   ↓ (calls)
 * PermissionHashQueryManager + PermissionClientManager + PermissionHashCommandManager
 * </pre>
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <p>2-Tier 캐시 전략: JWT Payload → Redis → AuthHub
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetPermissionHashService {

    private final PermissionHashCoordinator permissionHashCoordinator;

    public GetPermissionHashService(PermissionHashCoordinator permissionHashCoordinator) {
        this.permissionHashCoordinator = permissionHashCoordinator;
    }

    /**
     * Permission Hash 조회 (2-Tier Cache 전략)
     *
     * <p>1. JWT Payload의 permissionHash로 Redis 캐시 검증
     *
     * <p>2. Redis 캐시 없으면 AuthHub에서 조회 후 캐시
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param jwtPermissionHash JWT에 포함된 Permission Hash
     * @return Permission Hash
     */
    public Mono<PermissionHash> getPermissionHash(
            String tenantId, String userId, String jwtPermissionHash) {
        return permissionHashCoordinator.findByTenantAndUser(tenantId, userId, jwtPermissionHash);
    }
}
