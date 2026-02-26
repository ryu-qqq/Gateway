package com.ryuqq.gateway.application.authorization.manager;

import com.ryuqq.gateway.application.authorization.port.out.command.PermissionHashCommandPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Hash Command Manager (Reactive)
 *
 * <p>Redis Cache에 사용자별 Permission Hash를 저장하거나 삭제하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에 Permission Hash 저장
 *   <li>Redis Cache에서 Permission Hash 삭제 (캐시 무효화)
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionHashCommandPort - Redis Cache 저장/삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionHashCommandManager {

    private final PermissionHashCommandPort permissionHashCommandPort;

    public PermissionHashCommandManager(PermissionHashCommandPort permissionHashCommandPort) {
        this.permissionHashCommandPort = permissionHashCommandPort;
    }

    /**
     * Permission Hash 저장 (Redis Cache)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param permissionHash 저장할 Permission Hash
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> save(String tenantId, String userId, PermissionHash permissionHash) {
        return permissionHashCommandPort.save(tenantId, userId, permissionHash);
    }

    /**
     * Permission Hash 캐시 무효화
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> invalidate(String tenantId, String userId) {
        return permissionHashCommandPort.invalidate(tenantId, userId);
    }
}
