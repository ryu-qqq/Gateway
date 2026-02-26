package com.ryuqq.gateway.application.authorization.manager;

import com.ryuqq.gateway.application.authorization.port.out.query.PermissionHashQueryPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Hash 조회 Manager (Reactive)
 *
 * <p>Redis Cache에서 사용자별 Permission Hash를 조회하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Permission Hash 조회
 *   <li>Cache Hit 시 PermissionHash 반환
 *   <li>Cache Miss 시 빈 Mono 반환
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionHashQueryPort - Redis Cache 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionHashQueryManager {

    private final PermissionHashQueryPort permissionHashQueryPort;

    public PermissionHashQueryManager(PermissionHashQueryPort permissionHashQueryPort) {
        this.permissionHashQueryPort = permissionHashQueryPort;
    }

    /**
     * 사용자별 Permission Hash 조회 (Redis Cache)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Mono&lt;PermissionHash&gt; (Cache Miss 시 빈 Mono)
     */
    public Mono<PermissionHash> findByTenantAndUser(String tenantId, String userId) {
        return permissionHashQueryPort.findByTenantAndUser(tenantId, userId);
    }
}
